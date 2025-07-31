package org.rosi.modules ;

import java.io.*;
import java.util.* ;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class TailProducerModule extends RosiModule {

   private RosiCommandProcessor       _commandProcessor = null ; 
   private ModuleContext              _context = null ;
   private String                     _name    = null ;
   private File                       _tail    = null ;
   private long                       _delay   = 1000L ;

   public TailProducerModule( String moduleName , ModuleContext context ){
      super(moduleName,context);
      log("Initiating.");
      _name    = moduleName ;
      _context = context ;

      String tailName = _context.get("inputFile");
      if( tailName == null )
        throw new
        IllegalArgumentException("File name 'inputFile' not found in context");

      _tail = new File( tailName ) ;
      if( ! _tail.exists() )
        throw new
        IllegalArgumentException("File '"+tailName+"' not found");

      log("Input Filename : "+_tail );
      String tmp = _context.get("update" ) ;
      if( tmp != null )_delay = Long.parseLong( tmp ) ;
   } 

   public void run(){

      log("Starting.");
      
      try{
	       	    
         String input        = null ;

         long fileLength = _tail.length() ;
         long filePosition = fileLength ;

         while( true ){

            Thread.sleep( _delay );
            fileLength = _tail.length();
            if( fileLength < filePosition ){
                filePosition = fileLength ;
            }else if(fileLength > filePosition ){
                RandomAccessFile raf = new RandomAccessFile( _tail , "r" );
                raf.seek(filePosition);
                try{
                   while( ( input = raf.readLine() ) != null ){
                     if( _commandProcessor == null ){
                         put( new RosiCommand( input ) ) ;
                     }else{
                         RosiCommand com = _commandProcessor.process( input ) ;
                         if( com != null )put( com ) ;
                     }
                   }
                   filePosition = raf.getFilePointer();
                }catch(Exception abx ){
                   errorLog("Exception in main loop : "+abx );
                   throw abx ;
                }finally{
                   try{ raf.close() ; }catch(Exception xxxx){}
                }

            }

         } /* end while */

      }catch(Exception eee ){
         errorLog("Runtime Error : "+eee.getMessage() ) ;
      }

      log("Finished");

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
