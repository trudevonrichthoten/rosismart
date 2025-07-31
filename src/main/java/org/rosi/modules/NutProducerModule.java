package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.nut.*;


public class NutProducerModule extends RosiModule {

   private RosiCommandProcessor _commandProcessor = null ; 
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
   private File                 _file    = null ;
   private PatternTranslator    _filter  = null ;
   private NutUpsDriver         _ups     = null ;
   private String _hostName  = null ;
   private String _upsDevice = null ;
   private int    _port      = 0 ;
   private long   _sleepTime      = 1000L;
   private long   _forceBroadcast = 10 * _sleepTime ;


   public NutProducerModule( String moduleName , ModuleContext context  )
      throws Exception {

      super(moduleName,context);

      _context = context ;

      log( "Started");
 
      _hostName   = context.get("hostName"   , true ); 
      _upsDevice  = context.get("upsName"    , true ) ;
      _port       = Integer.parseInt(  context.get("portNumber" , true ) ) ; 
      _sleepTime  = Long.parseLong( context.get("sleepTime" , true ) ) * 1000L ;

      _ups = new NutUpsDriver( _hostName , _port ) ;
      _ups.addDeviceName( _upsDevice ) ;

      log( "Context created with hostname : "+_hostName+", port : "+_port+", device : "+_upsDevice);
   

      String x =  context.get("enforceBroadcast" , false ) ;
      if( x  != null ){
          try{

               _forceBroadcast = Long.parseLong( x ) * 1000L ;
           
          }catch(Exception ee ){
              String err = "Wrong format in value for 'enforceBroadcast' : "+x ;
              errorLog(err);
              throw new
              IllegalArgumentException(err);
          } 
          if(  _forceBroadcast < _sleepTime ){
              String err = "'enforceBroadcast' must be greater or equal to 'sleepTime'" ;
              errorLog(err);
              throw new
              IllegalArgumentException(err);
          } 
      }
      _forceBroadcast = 10 * _sleepTime ;

      log("'sleepTime'      set to "+_sleepTime+" millis");
      log("'forceBroadcast' set to "+_forceBroadcast+" millis");

      String filterFile = context.get( "filterFile" ) ;
      if( filterFile != null ){
         log("'filter file  : "+filterFile);
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("Filer file not found : "+ filterFile ) ;

         _filter = new PatternTranslator( f ) ;
      }


   } 
   public void run(){

       Map<String,String> remember = new HashMap<String,String>();

      long enforceTimer = System.currentTimeMillis() + _forceBroadcast;

       while(true){

          try{

             Map<String,String> map = _ups.getDeviceAttributes( _upsDevice ) ; 

             for( Map.Entry<String,String> e : map.entrySet() ){

                if( System.currentTimeMillis() > enforceTimer ){
                    enforceTimer = System.currentTimeMillis() + _forceBroadcast ;
                    remember.clear();
                }

                String key       = e.getKey() ;
                String value     = e.getValue() ;
 
                String lastValue = remember.get( key ) ; 

                if( ( lastValue != null ) && lastValue.equals( value ) )continue;

                remember.put( key , value ) ;

                if( _filter != null ){

                   String [] sub = _filter.translate( key+":"+value ) ;

                   if( ( sub == null ) || ( sub.length < 2 ) )continue ;
         
                   put( new RosiSetterCommand( sub[0] , sub[1] ) ) ;
                }
 
             }

             Thread.sleep( _sleepTime ) ;

          }catch(InterruptedException ieee ){
             errorLog("Was interrupted and stopped" ) ;
             break ;
          }catch(Exception eee ){
             errorLog( "Got exeception in main loop: "+eee ) ;
             if( isDebugMode() )eee.printStackTrace();
             break ;
          }
       }

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
