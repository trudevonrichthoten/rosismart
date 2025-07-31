package org.rosi.modules ;

import java.io.*;
import java.util.* ;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class TimeProducerModule extends RosiModule {

   private ModuleContext              _context = null ;
   private String                     _name    = null ;
   private long                       _delay   = 10000L ;

   public TimeProducerModule( String moduleName , ModuleContext context ){
      super(moduleName,context);
      log("Initiating with debug mode : "+isDebugMode());
      
      _name    = moduleName ;
      _context = context ;

      String delayString = _context.get("delay");
      if( delayString == null )
        throw new IllegalArgumentException("Delay name 'delay' not found in context");
  
      _delay = Long.parseLong( delayString ) ;
      
   } 

   public void run(){

      log("Starting.");

      try{

          while(true){
              Thread.currentThread().sleep( 1000L * _delay ) ;
              debug("Sending timer event");
              put( new RosiTimerCommand() ) ;
          }

      }catch(InterruptedException ieee ){
          errorLog("Interrupted in main loop : "+ieee.getMessage() ) ;
      }catch(Exception eee ){
          errorLog("Runtime Error in main loop : "+eee.getMessage() ) ;
      }

      log("Finished");

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
   }
}
