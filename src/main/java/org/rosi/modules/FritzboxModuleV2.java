package org.rosi.modules ;

import java.util.* ;
import java.io.*;
import java.net.*;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.drivers.fritzbox.*;

public class FritzboxModuleV2 extends RosiModule {

   private RosiCommandProcessor  _commandProcessor = null ; 
   private ModuleContext         _context   = null ;
   private RosiRuntimeExecution  _process   = null ;
   private PatternTranslator     _inFilter  = null ;
   private PatternTranslator     _outFilter = null ;

   private String _loginURI      = "/login_sid.lua" ;
   private String _serviceURI    = "/webservices/homeautoswitch.lua" ;

   private FbDriver        _fritzbox            = null ;
   private long            _fritzboxAliveSleep  =  2L * 60L * 1000L ;
   private long            _fritzboxQueryTime   =  1L * 60L * 1000L ;
   private long            _fritzboxForceUpdate = 10L * 60L * 1000L ;
   private long            _fritzboxForceTimer  = 0L ;

   public FritzboxModuleV2( String moduleName , ModuleContext context  )
      throws Exception
   {
      super(moduleName,context);

      log("Initiating. (FritzboxModuleV2)");

      _context = context ;

      _initializeFritzbox() ;

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

      String timers = context.get("fritzboxKeepalive");
      if( timers != null )_fritzboxAliveSleep = Long.parseLong( timers ) * 1000L ;      

      timers = context.get("fritzboxQuery");
      if( timers != null )_fritzboxQueryTime = Long.parseLong( timers ) * 1000L;      

      timers = context.get("enforceBroadcast");
      _fritzboxForceUpdate = timers != null ? 
                               ( Long.parseLong( timers ) * 1000L ) :
                               _fritzboxQueryTime * 10L ;      
 
      log("fritzboxKeepalive : "+_fritzboxAliveSleep+" millis");
      log("fritzboxQuery     : "+_fritzboxQueryTime+" millis");
      log("enforceBroadcast  : "+_fritzboxForceUpdate+" millis");
   } 
   private void _initializeFritzbox() throws Exception {

      String userString  = _context.get("user"     , true ) ; 
      String passString  = _context.get("password" , true ) ; 
      String urlString   = _context.get("URL"      , true ) ; 

      String dryrun = _context.get("dryrun" ) ;
      if( ( dryrun != null ) && ( dryrun.equals("yes") )){
         _fritzbox = null ;
         return ;
      }

      _fritzbox = new FbDriver( urlString , _loginURI , _serviceURI ) ;
      _fritzbox.setCredentials( userString , passString ) ;

      try{
          _fritzbox.authenticate() ;
      }catch(FileNotFoundException fnf ){
         errorLog("Server Error. URL not found at server : "+fnf.getMessage() ) ;
      }catch(HttpRetryException httpe ){
         int rc = httpe.responseCode() ;
         if( rc == 403 )errorLog("Authentication Failed" ) ;
         else errorLog("Login Failed : "+httpe.getMessage() ) ;
         throw httpe ;
      }catch(Exception ee ){
         errorLog("Contacting fritzbox failed due to : "+ee ) ;
         throw ee ;
      }

      new Thread( new KeepFritzboxAlive() , "Fritzbox keep alive" ).start() ; 

   } 
   public RosiSetterCommand createFritzboxCommand( RosiSetterCommand command )
       throws Exception
   {
 
      command = new RosiSetterCommand( command ) ;

      if( _outFilter != null ){

          String [] sub = _outFilter.translate( command.getKey() ) ;

          if( ( sub != null ) && ( sub.length >=1 ) )command.setKey( sub[0] ) ; 
          else return null ;
      }
      return command ;

   }
   private class KeepFritzboxAlive implements Runnable {

      public void run(){

         log("KeepFritzboxAlive started");
         try{
           
            while(true){

               Thread.sleep( _fritzboxAliveSleep ) ;
               log("Sending keep alive to fritzbox"); 
               try{
                  _fritzbox.getDeviceList() ;
               }catch(java.net.UnknownHostException uhe ){
                 errorLog(" UnknownHostException in keep alive thread (continuing) : "+uhe);
               }catch(java.net.HttpRetryException hrt ){

                 errorLog(" HttpRetryException in keep alive thread : "+hrt);
                 int rc = hrt.responseCode() ;
                 if( rc == 403 ){
                     errorLog(" HttpRetryException (403). No longer authenticated. Re-authenticating." ) ;
                     try{
                         _fritzbox.authenticate() ;
                         errorLog(" _fritzbox.authenticate() . Re-authentication successful." ) ;
                     }catch(HttpRetryException httpe ){
                         int rci = httpe.responseCode() ;
                         if( rci == 403 )errorLog(" HttpRetryException(304): Re-authentication Failed" ) ;
                         else errorLog(" HttpRetryException: Login Failed (retrying) :"+ 
                                       " details=[ "+httpe.getMessage()+" ]" ) ;
                     }catch(java.net.UnknownHostException uhe ){
                         errorLog(" UnknownHostException during re-authentication (continuing) : "+uhe);
                     }catch(Exception ee ){
                         errorLog(" Exception during re-authentication. Giving up.  details=[ "+ee+" ]" ) ;
                         throw ee ;
                     }
                  }

               }catch(Exception eee ){
                  throw eee ;
               }
            }
         }catch(Exception ee ){
            errorLog("Exception in keep alive thread: "+ee);
            errorLog("KeepFritzboxAlive stopped due to an error");    
         }
         log("KeepFritzboxAlive finished");
         
      }
   }
   private void executeFritzboxCommand( RosiSetterCommand command ) throws Exception {
  
      if( _fritzbox == null )return ;  

      String deviceName = command.getKey() ;
      String value      = command.getValue() ;

      String [] device = deviceName.split("$") ;
      if( device.length > 1 ){
         deviceName = device[0] ;
         String key = device[1] ;
 
         _fritzbox.setDevice( deviceName , key ,  value ) ; 

      }else{

         if( value.equals( "on" ) ){ 
            _fritzbox.setDevice( deviceName , 1 ) ;
         }else if( value.equals( "off" ) ){
            _fritzbox.setDevice( deviceName , 0 ) ;
         }else{
            try{
               Float f = Float.parseFloat( value ) * 2 ;
                _fritzbox.setDevice( deviceName , Math.round( f ) ) ;
            }catch( Exception ee ){
                errorLog("Argument of '"+deviceName+"' not a 'float'");
            }
         }
      }

   }
   public void run(){

      new Thread(
          new Runnable(){
              public void run(){
                 rosiToFritzbox() ;
              }
          } , "RosiToFritzbox"
      ).start();

      new Thread(
          new Runnable(){
              public void run(){
                 fritzboxToRosi() ;
              }
          } , "FritzboxToRosi" 
      ).start() ;

   }
   public void fritzboxToRosi(){

      log("Starting fritzboxToRosi thread.");

      Map<String,String> remember = new HashMap<String,String>() ;

      while(true){
          try{

                Thread.sleep( _fritzboxQueryTime ) ;

                queryFritzbox( remember ) ;

          }catch(InterruptedException ieee ){
             errorLog("Interrupted in main loop: "+ieee.getMessage() ) ;
             break ;
          }catch(Exception eee ){
             errorLog("Runtime Error in main loop : "+eee.getMessage() ) ;
             remember.clear();
          }
       }
   }
   private void queryFritzbox( Map<String,String> remember ) throws Exception {

      FbDeviceContainer container =  _fritzbox.getDeviceContainer();

      for(  FbDevice device : container.devices() ){

         if( System.currentTimeMillis() > _fritzboxForceTimer ){
              _fritzboxForceTimer = System.currentTimeMillis() + _fritzboxForceUpdate ;
              remember.clear();
         }

         String    basekey    = device.getDeviceID().replace(" " , "" ) ;

         for( Map.Entry<String,String> entry : device.getMap().entrySet() ){

            String key       = basekey+"."+entry.getKey() ;
            String value     = entry.getValue();

            String lastValue = remember.get( key ) ;
            if( ( lastValue != null ) && lastValue.equals( value ) )continue;

            remember.put( key , value ) ;

            log( "Received from device : "+key + " : " +value ) ;
            if( _inFilter != null ){

                String [] sub = _inFilter.translate( key+":"+value ) ;

                if( ( sub == null ) || ( sub.length < 2 ) )continue ;

                put( new RosiSetterCommand( sub[0] , sub[1] ) ) ;
                log( "Forwarded to BUS : "+sub[0] + " : " +sub[1] ) ;
            }
         }
      }
   } 
   public void rosiToFritzbox(){

      log("Starting rosiToFritzbox thread.");

      while(true){
          try{

             RosiCommand command  =  take() ;

             if( command instanceof RosiSetterCommand ){

                log("Setter '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

                try{
 
                   RosiSetterCommand fbc = new RosiSetterCommand( (RosiSetterCommand)command ) ;

                   if( _outFilter != null ){

                      String [] sub = _outFilter.translate( fbc.getKey() ) ;

                      if( ( sub != null ) && ( sub.length >=1 ) ){
                         fbc.setKey( sub[0] ) ; 
                         log("Sending to fritzbox : '"+fbc+"' from '"+command+"'");
                         executeFritzboxCommand( fbc ) ;
                      }
                   }

                }catch(Exception ee ){
                   errorLog( "Exception executing fritzbox command '"+command+"' : "+ee ); 
                }
             }else{
                log("unkown (ignored) '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;
             }

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
