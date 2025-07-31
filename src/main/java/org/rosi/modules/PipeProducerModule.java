package org.rosi.modules ;

import java.io.*;
import java.util.* ;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class PipeProducerModule extends RosiModule {

   private RosiCommandProcessor       _commandProcessor = null ; 
   private ModuleContext              _context = null ;
   private String                     _name    = null ;
   private File                       _pipe    = null ;

   public PipeProducerModule( String moduleName , ModuleContext context ){
      super(moduleName,context);
      log("Initiating.");
      _name    = moduleName ;
      _context = context ;

      String pipeName = _context.get("pipe");
      if( pipeName == null )
        throw new IllegalArgumentException("Pipe name 'pipe' not found in context");

      _pipe = new File( pipeName ) ;
      if( ! _pipe.exists() )
        throw new
        IllegalArgumentException("Pipe '"+pipeName+"' not found");
   } 

   public void run(){

      log("Starting.");

      
      try{
	       	    
         String input = null ;
	         
         while( true ){

            BufferedReader reader = new BufferedReader( new FileReader( _pipe ) ) ;

            //debug( "Reader opened : "+_pipe);
            try{
                   while( ( input = reader.readLine() ) != null ){
                     if( _commandProcessor == null ){
                         put( new RosiCommand( input ) ) ;
                     }else{
                         RosiCommand com = _commandProcessor.process( input ) ;
                         if( com != null )put( com ) ;
                     }
                   }
            }catch(Exception abx ){
                    errorLog("Exception in pipe : "+abx );
                    try{ reader.close() ; }catch(Exception xxxx){}
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
