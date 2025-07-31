package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.rscm.*;

public class RscmConsumerModule extends RosiModule implements RscmClient.RscmMessageArrivable {
                       
   private RosiCommandProcessor _commandProcessor = null ; 
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
   private PatternTranslator    _filter  = null ;
   private RscmClient           _rscm    = null ;

   public RscmConsumerModule( String moduleName , ModuleContext context  )
      throws Exception {

      super(moduleName,context);

      log( "Started");

      _context = context ;
      
      _rscm    = new RscmClient() ;

      String serverDetails = context.get("server" , true );
      
      String [] servers = serverDetails.split(",") ;
      for( int i = 0 ; i < servers.length ; i++ ){
          String details = servers[i].trim() ;
          if( details.length() == 0 )continue ;
          
          String [] d = details.split(":") ;
          if( d.length != 2 )
              throw new
              IllegalArgumentException("Rscm Server details syntax error <server>:<port> : "+details);
          
          String serverName = d[0] ;
          int serverPort = 0 ;
          try{
              serverPort = Integer.parseInt( d[1] ) ;
          }catch(Exception ee ){
              throw new
              IllegalArgumentException("Rscm Server details syntax error (port not a number): "+details);
          }
          _rscm.addServer( serverName , serverPort ) ;
          log("Server added : "+serverName+":"+serverPort);
      }

      String filterFile = context.get( "filterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("Filer file not found : "+ filterFile ) ;

         _filter = new PatternTranslator( f ) ;
      }


   } 
   public void messageArrived(  RscmClient.Envelope e ){
                           
      if( e.isTimeout() ){
          log("Set device request timed out");
      }else{
          if( e instanceof RscmClient.SetDeviceEnvelope ){
              RscmClient.SetDeviceEnvelope se = (RscmClient.SetDeviceEnvelope)e;
              int rc = se.getReturnValue() ;
              if( rc != 0 ){
                  String msg = se.getReturnMessage();
                  errorLog("Problem setting rscm device : "+msg);
              }
          }
      }
   }
   public void run(){
       while(true){

          try{

             RosiCommand c  = take() ;

             RosiSetterCommand command = new RosiSetterCommand( (RosiSetterCommand) c ) ; 

             if( _filter != null ){

                String [] sub = _filter.translate( command.getKey()+":"+command.getValue() ) ;

                if( ( sub != null ) && ( sub.length == 2 ) ){
                     command.setKey( sub[0] ) ;
                     command.setValue( sub[1] ) ;
                }
                else continue ;

 
             }
             try{ 
               String device = command.getKey() ;
               String option = command.getValue() ;
              
               _rscm.sendSetDeviceRequest( 
                      device , 
                      option , 
                      10000L ,
                      this 
              ) ;
               
             }catch(Exception ee ){
                 errorLog("Set device command failed : "+ee.getMessage());
                 throw ee ;
             }


          }catch(InterruptedException ieee ){
             errorLog("Was interrupted and stopped" ) ;
             break ;
          }catch(Exception eee ){
             errorLog( "Got exeception in main loop: "+eee ) ;
          }
       }

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
