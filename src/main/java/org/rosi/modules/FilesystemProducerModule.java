package org.rosi.modules ;

import java.io.*;
import java.util.* ;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class FilesystemProducerModule extends RosiModule {

   private RosiCommandProcessor       _commandProcessor = null ; 
   private File                       _directory        = null ;
   private long                       _delay            = 1000L ;
   private Map<String,FileContext>    _list = new HashMap<String,FileContext>() ;

   private class FileContext {
      private String _name = null ;
      private long   _modified = 0L ;
      private FileContext( String name ){
         _name      = name ;
      }
      private FileContext( String name , long modified ){
         _name      = name ;
         _modified = modified ;
      }
      private long getLastModified(){
        return _modified ;
      }
      private void setLastModified( long modified ){
        _modified  = modified ;
      }
   }
   public FilesystemProducerModule( String moduleName , ModuleContext context ){

      super(moduleName,context);

      log("Initiating.");

      String dirName = getContext("flagDirectory");
      if( dirName == null )
        throw new
        IllegalArgumentException("Directory 'flagDirectory' not found in context");

      _directory = new File( dirName ) ;
      if(  ( ! _directory.exists() ) || ( ! _directory.canRead() ) )
        throw new
        IllegalArgumentException("Directory '"+dirName+"' not found");

      log("Using flag directory "+_directory );

      String tmp = _context.get("update" ) ;
      if( tmp != null )_delay = Long.parseLong( tmp ) ;

      log("Update period : "+_delay+" ms" );
   } 
   public void run(){

      log("Starting.");
      
      try{
	       	    
         while( ! Thread.interrupted() ){

            Thread.sleep( _delay );
            try{

               File [] fileList = _directory.listFiles() ;

               for( int i = 0 ; i < fileList.length ; i++ ){

                   File file = fileList[i] ;
                   if( ! file.isFile() )continue ; 
                   String fileName = file.getName() ;

                   FileContext context = _list.get( fileName ) ;
                   if( context == null ){
                       _list.put( fileName , context = new FileContext( fileName ) ) ;
                       log("New flag file found : "+fileName) ;
                   }
                   long modified = file.lastModified()  ;
                   if( modified > context.getLastModified() ){
                       String fileContent = getFileContent( file ) ;
                       if( ( fileContent == null ) || ( fileContent.equals("") ) ){
                          errorLog("Flag File is empty : "+fileName ) ;
                       }else{
                          put( new RosiSetterCommand( fileName , fileContent ) ) ;
                       }
                       context.setLastModified(modified);
                   }
                    
               }

            }catch(Exception abx ){
               errorLog("Exception in main loop : "+abx );
            }

         } /* end while */

      }catch( InterruptedException eee ){
         errorLog("We were interrupted.") ;
      }

      log("Finished");

   }
   private String getFileContent( File file ) throws IOException {
       
       BufferedReader br = new BufferedReader( new FileReader( file ) ) ;

       try{
 
           String result = br.readLine() ;

           if( result == null )return null ;

           return result.trim();

       }finally{
           try{ br.close() ; }catch(Exception ee ){}
       }
   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
   public static void main( String [] args ) throws Exception {
          File [] f = new File( args[0] ).listFiles() ;
          for( int i = 0 ; i < f.length ; i++ ){ 
             System.out.println("Files : " + f[i] ) ;
             System.out.println("Names : " + f[i].getName() ) ;
          }
   }
}
 
