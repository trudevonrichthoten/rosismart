package org.rosi.modules ;

import java.io.*;
import java.util.* ;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class StdinProducerModule extends RosiModule {

   private RosiCommandProcessor _commandProcessor = null ; 
   private boolean              _doSimulation     = false ;

   public StdinProducerModule( String moduleName , ModuleContext context ){
      super(moduleName,context);
      log("Initiating.");

      String tmp = getContext("simulation") ;  

      _doSimulation =
          ( tmp != null ) && ( tmp.equals("yes") || tmp.equals("true") ) ;

   } 

   public void run(){
      log("Starting.");

      if( _doSimulation )new Thread(  new Receiver() ).start() ;

      BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) ) ;
      
      try{
	       	    
         String input = null ;
	         
         while( ( input = reader.readLine() ) != null ){
           if( _commandProcessor == null ){
               put( new RosiCommand( input ) ) ;
           }else{
               RosiCommand com = _commandProcessor.process( input ) ;
               if( com != null )put( com ) ;
           }
         }

      }catch(InterruptedException ieee ){
         errorLog("Interrupted: "+ieee.getMessage() ) ;
      }catch(Exception eee ){
         errorLog("Runtime Error : "+eee.getMessage() ) ;
      }

      log("Finished");

   }
   private class Receiver implements Runnable {

     public void run()
     {
          log("Starting receiver");
          while( ! Thread.interrupted() ){
             try{
                 RosiCommand command = take() ; 
                 System.out.println("--> "+command.toString());
             }catch(InterruptedException ee ){
                 break ;
             }catch(Exception ee ){
                 errorLog("Error: "+ee.toString());
             }
         }

     }
   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
