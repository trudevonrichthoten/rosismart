package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class LogFileConsumerModule extends RosiModule {

   private RosiCommandProcessor _commandProcessor = null ; 
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");
   private File                 _file    = null ;
   private PrintWriter          _writer  = null ;


   public LogFileConsumerModule( String moduleName , ModuleContext context  )
      throws IllegalArgumentException, IOException {

      super(moduleName,context);

      _context = context ;

      log( "Started");

      String logFileName = context.get("logFile" , true );

      _file = new File( logFileName ) ;

      /* File needs to exist */
      if( ! _file.getParentFile().canWrite() )
         throw new
         IllegalArgumentException( "Can't write to : "+_file ) ; 

      _writer = new PrintWriter( new FileWriter( _file , true ) ) ;
      _writer.println(_sdf.format(new Date())+" "+moduleName+" started");
      _writer.flush();

   } 

   public void run(){
     try{
       while(true){

          try{

             RosiCommand command  =  take() ;

             _writer.println(_sdf.format(new Date())+" "+command.getSource()+" "+command);
             _writer.flush();

          }catch(InterruptedException ieee ){
             errorLog("Was interrupted and stopped" ) ;
             throw ieee ;
          }catch(Exception eee ){
             errorLog( "Got exeception in main loop: "+eee ) ;
          }
       }
     }catch(Exception iee ){
     }finally{
       try{ _writer.close() ; }catch(Exception ie){}
     }

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
