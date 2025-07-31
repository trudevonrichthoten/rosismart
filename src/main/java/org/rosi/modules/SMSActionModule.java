package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.modem.*;


public class SMSActionModule extends RosiModule {

   private class SMSContext {

      private String accessPin           = null  ;
      private String defaultPhoneNumber  = null ;
      private SMSDriver smsDriver        = null ;

      private SMSContext( String portName , String pin ) throws Exception {
         this.accessPin = pin ;
         this.smsDriver = new SMSDriver( portName , pin ) ;
      }
      
      private synchronized void sendMessage( String phoneNumber , String message ) throws Exception{
         this.smsDriver.sendSMS( this.accessPin , phoneNumber , message ) ;
      }
      private synchronized void sendMessage( String message ) throws Exception{
         this.smsDriver.sendSMS( this.accessPin , this.defaultPhoneNumber , message ) ;
      }
      private synchronized void clearMessages() throws Exception{
         this.smsDriver.clearSMSs( this.accessPin ) ;
      }
      private synchronized TextMessage [] getNewMessages() throws Exception {
         try{
            return this.smsDriver.fetchSMSs();
         }catch( Exception ee ){
            log("smsDriver.fetchSMSs reports : "+ee );
            throw ee ;
         }
      }
      private void close(){
          try{
              this.smsDriver.close();
          }catch(Exception ee ){
              errorLog("Shutdown smsDriver resulted in : "+ee );
          }
      }
   }
   private RosiCommandProcessor _commandProcessor = null ; 
   private ModuleContext        _context     = null ;
   private SimpleDateFormat     _sdf         = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
   private File                 _file        = null ;
   private PatternTranslator    _inFilter    = null ;
   private PatternTranslator    _outFilter   = null ;
   private long                 _sleepMillis = 10000L ;

   private SMSContext _smsContext = null ;

   public SMSActionModule( String moduleName , ModuleContext context  )
      throws Exception {

      super(moduleName,context);

      _context = context ;

      log( "Started");
 
      String portName = context.get("portName" , true ); 
      String pin      = context.get("pin" , true ) ;

      _smsContext = new SMSContext( portName , pin ) ;

      log( "Context created with "+portName+" and "+pin);

      _smsContext.defaultPhoneNumber = context.get("phoneNumber" , true ) ;
 
      log( "Default phone number is : "+_smsContext.defaultPhoneNumber );

      String sleepTime = context.get("reveiveSMSUpdateTime");
      if( sleepTime != null ){
          _sleepMillis = Long.parseLong(sleepTime ) * 1000L ;
      }
      log( "Receive SMS Update Time set to : "+_sleepMillis+" millis");

      String filterFile = context.get( "inFilterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("'inFilterFile' file not found : "+ filterFile ) ;

         _inFilter = new PatternTranslator( f ) ;
      }
      filterFile = context.get( "outFilterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("'outFilterFile' file not found : "+ filterFile ) ;

         _outFilter = new PatternTranslator( f ) ;
      }

      String clearSMS = context.get("clearSMS");
      if( ( clearSMS != null ) && ( clearSMS.equals("true") ) ){
         _smsContext.clearMessages() ;
         log( "Clearing remaining text messages on request");
      }

   } 
   private String buildMessage( String key , String value ){
  
      StringBuffer sb = new StringBuffer() ;
 
      sb.append(_sdf.format( new Date() )).append(";").
         append(key).append(";").
         append(value).append(";");

      return sb.toString();

   } 
   private Thread _networkToRosiThread = null ;
   private Thread _rosiToNetworkThread = null ;
   private synchronized void shutdown(){

       if( _networkToRosiThread != null )_networkToRosiThread.interrupt() ;
       _networkToRosiThread = null ;

       if( _rosiToNetworkThread != null )_rosiToNetworkThread.interrupt() ;
       _rosiToNetworkThread = null ;

       _smsContext.close() ;
   }
   public synchronized void run(){
 
      _networkToRosiThread = 
      new Thread(
          new Runnable(){
              public void run(){
                 runNetworkToRosi() ;
              }
          }
      ); 
      _networkToRosiThread.start() ;

      _rosiToNetworkThread = 
      new Thread(
          new Runnable(){
              public void run(){
                 runRosiToNetwork() ;

              }
          }
      ) ;
      _rosiToNetworkThread.start() ;
   }
   public void runNetworkToRosi(){
       log(" started : NetworkToRosi");
       while( ! Thread.interrupted() ){

          try{ 

              Thread.sleep( _sleepMillis ) ;

              TextMessage [] messages = _smsContext.getNewMessages() ; 

              for( int i = 0 ; i < messages.length ; i++ ){

                 debug("runNetworkToRosi: received : "+messages[i] );
 
                 try{

                     processingIncomingMessage( messages[i] ) ;

                 }catch(Exception iee ){
                    debug("runNetworkToRosi: received : "+messages[i] );
                    errorLog("runNetworkToRosi: error in processing message : "+iee);
                 }
              }

          }catch(InterruptedException ieee ){
             errorLog("runNetworkToRosi: Was interrupted and stopped" ) ;
             break ;
          }catch(Exception eee ){
             errorLog( "runNetworkToRosi: Got exeception in main loop: "+eee ) ;
             eee.printStackTrace();
             break ;
          }
 
       }
       errorLog("runNetworkToRosi requests shutdown");
       shutdown() ;
   }
   private void processingIncomingMessage( TextMessage msg )throws Exception {

       String phone = msg.getPhoneNumber() ;

       if( phone.length() < 2 ){
          phone = "UNKOWN" ;
       }else{
          phone = phone.substring( 1 , phone.length()-1 ) ;
       }

       String body = msg.getBody() ;

       if( body == null )
          throw 
          new IllegalArgumentException("Empty message received") ;

       String [] elements = body.split("#");
       if( ( elements.length == 0 ) || ( elements.length > 2 ) )
          throw 
          new IllegalArgumentException("Illegal number of elements received : "+elements.length) ;

       String key   = elements[0].trim().toLowerCase() ;
       String value = elements.length == 1 ? "on" : elements[1].trim().toLowerCase() ;

       if( ( key.length() == 0 ) || ( value.length() == 0 ) )
          throw 
          new IllegalArgumentException("Illegal formatted input : Either key of value has zero size") ;

       debug("processIncomingMessage: >"+phone+"< >"+key+"< >"+value+"<");

       if( _outFilter != null ){

            String [] sub = _outFilter.translate( phone+":"+key+":"+value ) ;

            if( ( sub == null ) || ( sub.length == 0 ) ){
               debug("processingIncomingMessage: no match in outFilter file");
               return ;
            }

            for( int i = 0 ; i < sub.length ; i++ ){
               debug( "SUB["+i+"] : >"+sub[i]+"<");
            }
            if(  sub.length < 2 ){
               debug("processingIncomingMessage: not enough arguments in 'outFilter'");
               return ;
            }
            put( new RosiSetterCommand(  sub[0] , sub[1] ) ) ;
       }
   } 
   public void runRosiToNetwork(){

       log(" started : RosiToNetwork");

       while( ! Thread.interrupted() ){

          try{

             RosiCommand c  = take() ;
 
             if( ! ( c instanceof RosiSetterCommand ) ){
                 errorLog("Received an unexpected command type : "+c.getClass().getName() ) ;
                 continue ;
             }

             RosiSetterCommand command = new RosiSetterCommand( (RosiSetterCommand) c ) ; 

             debug("SMS received from dispatcher : "+command);
             if( _inFilter != null ){

                String [] sub = _inFilter.translate( command.getKey()+":"+command.getValue() ) ;

                if( ( sub == null ) || ( sub.length == 0 ) )continue ;

                for( int i = 0 ; i < sub.length ; i++ ){
                  debug( "SUB["+i+"] : >"+sub[i]+"<");
                }
                command.setKey( sub[0] ) ;
                if( sub.length > 1 )command.setValue( sub[1] ) ;
 
                if( sub.length > 2 ){
                   /*
                    * phone number was givin in filter file.
                    */
                   debug("SMS ("+sub[2]+") : "+command);

                   _smsContext.sendMessage(
                           sub[2] ,
                           buildMessage( command.getKey() , command.getValue() )
                                          ) ; 
                }else{

                   debug("SMS (default phone) : "+command);

                   _smsContext.sendMessage( 
                           buildMessage( command.getKey() , command.getValue() )
                                          ) ; 
                } 
 
             }

          }catch(InterruptedException ieee ){
             errorLog("runRosiToNetwork: Was interrupted and stopped" ) ;
             break ;
          }catch(Exception eee ){
             errorLog( "runRosiToNetwork: Got exeception in main loop: "+eee ) ;
             eee.printStackTrace();
             break ;
          }
       }

       errorLog("runRosiToNetwork requests shutdown");
       shutdown() ;

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
