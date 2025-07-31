package org.rosi.modules ;

import org.rosi.util.*;

public class DummyConsumerModule extends RosiModule {

   private RosiCommandProcessor       _commandProcessor = null ; 
   private ModuleContext              _context = null ;

   public DummyConsumerModule( String moduleName , ModuleContext context  ){
      super(moduleName,context);
      log("Initiating.");
      _context = context ;
   } 
   public void run(){
      log("Starting.");
       while(true){
          try{

             RosiCommand command  =  take() ;

             log("'"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

          }catch(InterruptedException ieee ){
             errorLog("Interrupted in main loop: "+ieee.getMessage() ) ;
             break ;
          }catch(Exception eee ){
             errorLog("Runtime Error in main loop : "+eee.getMessage() ) ;
          }
       }
   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
