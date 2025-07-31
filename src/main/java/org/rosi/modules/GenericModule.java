package org.rosi.modules ;

import java.util.* ;
import java.io.*;
import java.net.*;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.generic.*;

public abstract class GenericModule extends RosiModule {

   private RosiCommandProcessor  _commandProcessor = null ; 
   private ModuleContext         _context   = null ;
   private RosiRuntimeExecution  _process   = null ;
   private PatternTranslator     _inFilter  = null ;
   private PatternTranslator     _outFilter = null ;

   private XGenericDriver  _driver            = null ;
   private long            _driverAliveSleep  =  2L * 60L * 1000L ;
   private long            _driverQueryTime   =  1L * 60L * 1000L ;
   private long            _driverForceUpdate = 10L * 60L * 1000L ;
   private long            _driverForceTimer  = 0L ;

   private boolean _rosiToServerThread = true ;
   private boolean _serverToRosiThread = true ;
   private boolean _keepaliveThread    = true ;

   private static String __deviceKeyDelimiter = "#" ;

   public GenericModule( String moduleName , ModuleContext context  )
      throws Exception
   {
      super(moduleName,context);

      log("Initiating. (GenericModuleV1)");

      _context = context ;

      initialize() ;

      String filterFile = context.get( "outFilterFile" ) ;
      if( filterFile == null )filterFile = context.get( "filterFile" ) ;

      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("Out Filter file not found : "+ filterFile ) ; 
 
         _outFilter = new PatternTranslator( f ) ;
      }
      filterFile = context.get( "inFilterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("In Filter file not found : "+ filterFile ) ; 
 
         _inFilter = new PatternTranslator( f ) ;
      }

      String timers = context.get("keepalive");
      if( timers != null )_driverAliveSleep = Long.parseLong( timers ) * 1000L ;      

      timers = context.get("query");
      if( timers != null )_driverQueryTime = Long.parseLong( timers ) * 1000L;      

      timers = context.get("enforceBroadcast");
      _driverForceUpdate = timers != null ? 
                               ( Long.parseLong( timers ) * 1000L ) :
                               _driverQueryTime * 10L ;      
 
      log("keepalive : "+_driverAliveSleep+" millis");
      log("query     : "+_driverQueryTime+" millis");
      log("enforceBroadcast  : "+_driverForceUpdate+" millis");

   } 
   protected void setDriver( XGenericDriver driver ){
      _driver = driver ;
   } 
   public void setThreads( boolean toServer , boolean fromServer , boolean keepAlive ){
      _rosiToServerThread = toServer ;
      _serverToRosiThread = fromServer ;
      _keepaliveThread    = keepAlive ;
      log("Thread setup : toServer="+_rosiToServerThread+
                        ";fromServer="+_serverToRosiThread+
                        ";keepAlive="+_keepaliveThread );
   
   }
   public abstract void initialize() throws Exception ;
   public abstract boolean handleException( Exception eee ) ;

   private class KeepDriverAlive implements Runnable {

      public void run(){

         log("Starting keepAlive thread.");
         try{
           
            while(true){

               Thread.sleep( _driverAliveSleep ) ;

               try{

                  log("Sending keep alive to server"); 

                  _driver.update() ;

               }catch( Exception eee ){
                  errorLog("Exception in keep alive thread (continuing) : "+eee);
                  eee.printStackTrace() ;
                  if( handleException( eee ) )throw eee ;
               }
            }
         }catch(InterruptedException ieee ){
             errorLog("Interrupted in main loop: "+ieee.getMessage() ) ;
         }catch(Exception ee ){
            errorLog("Exception in keep alive thread: "+ee);
            errorLog("KeepDriverAlive stopped due to an error");    
         }
         log("KeepAlive finished");
         
      }
   }
   private void executeServerCommand( RosiSetterCommand command ) throws Exception {
  
      if( _driver == null )return ;  

      String deviceName = command.getKey() ;
      String value      = command.getValue() ;

      String [] device = deviceName.split(__deviceKeyDelimiter) ;

      if( device.length > 1 ){

         deviceName = device[0] ;
         String key = device[1] ;
         _driver.setDeviceAttribute( deviceName , key ,  value ) ; 

      }else{
         String key = device[0] ;
         _driver.setDeviceAttribute( "default" , key , value ) ; 
      }

   }
   public void run(){

      Thread thread = new Thread(
          new Runnable(){
              public void run(){
                 rosiToServer() ;
              }
          } , "RosiToServer"
      );
      if( _rosiToServerThread )thread.start() ;

      thread = new Thread(
          new Runnable(){
              public void run(){
                 serverToRosi() ;
              }
          } , "ServerToRosi" 
      ) ;
      if( _serverToRosiThread )thread.start() ;

      thread = new Thread( 
          new KeepDriverAlive() , "Driver keep alive"
      ) ; 
      if( _keepaliveThread )thread.start() ;

   }
   public void serverToRosi(){

      log("Starting serverToRosi thread.");

      Map<String,String> remember = new HashMap<String,String>() ;

      while(true){
          try{

                Thread.sleep( _driverQueryTime ) ;

                queryServer( remember ) ;

          }catch(InterruptedException ieee ){
             errorLog("Interrupted in main loop: "+ieee.getMessage() ) ;
             break ;
          }catch(Exception eee ){
             errorLog("Runtime Error in serverToRosi loop : "+eee.getMessage() ) ;
             remember.clear();
             if( handleException( eee ) )break ;
          }
      }
      log("Finishing serverToRosi thread.");
   }
   private void queryServer( Map<String,String> remember ) throws Exception {
      debug("Querying Driver for attributes.");
      if( _driver == null )return ;

      _driver.update() ;

      List<String> deviceNames = _driver.getDeviceNames() ;

      for(  String deviceName : deviceNames ){

         if( System.currentTimeMillis() > _driverForceTimer ){
              _driverForceTimer = System.currentTimeMillis() + _driverForceUpdate ;
              remember.clear();
         }

         String    basekey    = deviceName.replace(" " , "" ) ;

         try{
           for( Map.Entry<String,String> entry : _driver.getDeviceAttributes(deviceName).entrySet() ){

              String key       = basekey+__deviceKeyDelimiter+entry.getKey() ;
              String value     = entry.getValue();

              String lastValue = remember.get( key ) ;
              if( ( lastValue != null ) && lastValue.equals( value ) )continue;

              remember.put( key , value ) ;

              debug( "From Dev : ("+key + " : " +value+")" ) ;
  
              if( _inFilter != null ){

                  String [] sub = _inFilter.translate( key+":"+value ) ;

                  if( ( sub == null ) || ( sub.length < 2 ) )continue ;

                  put( new RosiSetterCommand( sub[0] , sub[1] ) ) ;
                  log( "Dev to Bus : ("+sub[0] + " : " +sub[1]+") <- ("+key + " : " +value+")" ) ;
              }
           }
        }catch( Exception ee ){
           errorLog("Couldn't find device '"+deviceName+"' in map  (There is a bug somewhere!)");
        }
      }
   } 
   public void rosiToServer(){

      log("Starting rosiToServer thread.");

      while(true){
          try{

             RosiCommand command  =  take() ;

             if( command instanceof RosiSetterCommand ){

                debug("From Bus : "+command ) ;

                try{
 
                   RosiSetterCommand fbc = new RosiSetterCommand( (RosiSetterCommand)command ) ;

                   if( _outFilter != null ){

                      String [] sub = _outFilter.translate( fbc.getKey() ) ;

                      if( ( sub != null ) && ( sub.length >=1 ) ){
                         fbc.setKey( sub[0] ) ; 
                         log("Bus to Dev : "+fbc+" <- "+command);
                         executeServerCommand( fbc ) ;
                      }
                   }

                }catch(Exception ee ){
                   errorLog( "Exception executing server command '"+command+"' : "+ee ); 
                   throw ee ;
                }
             }else{
                log("unkown (ignored) '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;
             }

          }catch(InterruptedException ieee ){
             errorLog("Interrupted in main loop: "+ieee.getMessage() ) ;
             break ;
          }catch(Exception eee ){
             errorLog("Runtime Error in rosiToServer loop : "+eee.getMessage() ) ;
             if( handleException( eee ) )break ;
          }
      }
      log("Finishing rosiToServer thread.");

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
