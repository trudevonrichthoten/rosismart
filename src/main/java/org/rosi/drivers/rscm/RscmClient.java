package org.rosi.drivers.rscm;
/* 
 * Version 2
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class RscmClient {


   
   public static void outDebug( String msg ){
      System.out.println("DEBUG ("+System.currentTimeMillis()+","+Thread.currentThread().getName()+") : "+msg ) ;   
   }
   public interface RscmMessageArrivable {
   /* ----------------------------------- */
        public void messageArrived( Envelope e ) ;
         
   }
/*
         String request   = "rscm:1:"+nextId+":setdevice:"+device+":"+option ;
         byte [] sendData = request.getBytes();
*/
   public class SetDeviceEnvelope extends Envelope {
   /* ---------------------------------------------------- */
       private String _device = null ;
       private String _option = null ;
       private int    _returnValue   = -1 ; 
       private String _returnMessage = "Internal Client Error" ;
       private SetDeviceEnvelope( ServerInfo info , String device , String option ){
          super(info);
          _device = device ;
          _option = option ;
       }
       public String getPayload(){ return "setdevice:"+_device+":"+_option ; } 
       public void replyArrived(){
           String [] reply = getResponsePayload();
           outDebug("Reply arrived : vector size = "+reply.length);
           if( reply.length < 1 ){
               _returnMessage = "No arguments from server." ;
               _returnValue   = -1004 ;
               return ;
           }
           try{
                _returnValue = Integer.parseInt( reply[0] ) ; 
           }catch(Exception ee){
                _returnMessage = "Return code from server not an integer value : "+reply[0] ;
                _returnValue   = -1005 ;
                return ;
           }
           _returnMessage = reply.length < 2 ? "No Error Message" :  reply[1]  ;
           return ;
       }
       public int getReturnValue(){  return _returnValue ; }
       public String getReturnMessage(){  return _returnMessage ; }
   }
   /*
    *
    *
    */
   public void sendSetDeviceRequest( String deviceName , 
                                     String deviceOption , 
                                     long waitTime ) throws Exception {
/* ----------------------------------------------------------------------- */

         sendSetDeviceRequest( deviceName , deviceOption , waitTime , null ) ;
   }
   /*
    *
    *
    */
   public void sendSetDeviceRequest( String deviceName , 
                                     String deviceOption , 
                                     long waitTime ,
                                     RscmMessageArrivable callback   ) throws Exception {
/* ----------------------------------------------------------------------- */
      
       
       String [] device = deviceName.split("\\.") ;
       if( device.length < 2 )
              throw new
              Exception("ServiceName not found in device name : "+deviceName+" size : "+device.length);
     
       ServerInfo info = _serverMap.get( device[0] ) ;      
       if( info == null )
              throw new
              Exception("Server not found for : >"+device[0]+"< : "+_serverMap);
              
      outDebug("Server found for : "+deviceName );        
      SetDeviceEnvelope e = new SetDeviceEnvelope(info,deviceName, deviceOption ) ;
      
      e.setCallback( callback ) ;
      
      sendRequest( e , waitTime ) ;
      
      if( e.getCallback() != null )return ;
      
      if( e.isTimeout() )
          throw new
          Exception("Request timed out");
          
      int rc = e.getReturnValue() ;
      if( rc != 0 ){
          String rm = e.getReturnMessage() ;
          throw new
          Exception( rm == null ? ( "Server error : "+rc ) : ( "("+rc+") "+rm ) ) ;
      }
      return ;
   }    
/*
    =========================================================================== 
    =========================================================================== 
    =========================================================================== 
    =========================================================================== 
    =========================================================================== 
*/
   private class GetServiceNameEnvelope extends Envelope {
 /* ---------------------------------------------------- */
       private String _serviceName = null ;
       private GetServiceNameEnvelope( ServerInfo info ){
          super(info);
       }
       public String getPayload(){ return "ping:" ; } 
       public void replyArrived(){
           String [] reply = getResponsePayload();
           outDebug("Reply arrived : vector size = "+reply.length);
           if( reply.length < 2 )return ;
           _serviceName = reply[1] ;          
       }
       public String getServiceName(){ return _serviceName ; }
   }
   private class Housekeeping  implements Runnable {

      public void run(){
         synchronized( _serverList ){
            while(true){
               try{
                   
                  for( ServerInfo info : _serverList ){
                      doTheHousekeeping( info ) ;
                      _serverList.wait( 1000L ) ;
                  }
                  _serverList.wait( 10000L ) ;
                                   
               }catch(Exception ee ){
                  outDebug("Housekeeping loop interruped : "+ee );
                  ee.printStackTrace();
                  break ;
               }
            }
         }

      }
      private void doTheHousekeeping(ServerInfo info ) throws Exception {
          
          GetServiceNameEnvelope e = new GetServiceNameEnvelope(info) ;
          try{
             sendRequest( e , 10000L ) ;
             info._serviceName = e.getServiceName() ;
             if( info._serviceName == null ){
                outDebug("Service name request timed out or was 'null'");
             }else{
                _serverMap.put( info._serviceName , info );
                outDebug("Service Name set : "+e._serviceName);
             }
          }catch(Exception ee ){
             outDebug("Exception in sending request : "+ee.getMessage()); 
             if( info._serviceName != null ){
                outDebug("removing service : "+info._serviceName); 
                _serverMap.remove(   info._serviceName ) ;
                info._serviceName = null ;
             }
          }
      }
   }            
/*
    =========================================================================== 
    =========================================================================== 
    =========================================================================== 
    =========================================================================== 
    =========================================================================== 
*/
   private Map<Long,Envelope>     _requestMap = new HashMap<Long,Envelope>() ;
   private Map<String,ServerInfo> _serverMap  = new HashMap<String,ServerInfo>() ;
   private List<ServerInfo>       _serverList = new ArrayList<ServerInfo>();
   private DatagramSocket _clientSocket = null ;
   private long           _counter      = 100 ;
   private Timer          _timer        = new Timer("timer");
   /*
    *
    *
    */
   public void sendRequest( Envelope e , long waitTime ) throws Exception {
/* ----------------------------------------------------------------------- */
      synchronized( _requestMap ){

         long nextId = _counter++ ;

         e.setMessageId( nextId ) ;

         byte [] payload = e.getMessage().getBytes() ;

         DatagramPacket sendPacket = 
              new DatagramPacket( payload , 
                                  payload.length, 
                                  e._serverInfo._IPAddress, 
                                  e._serverInfo._port );

         outDebug("Sending message : "+e.getMessage());
         _requestMap.put( nextId ,  e ) ; 
         _clientSocket.send(sendPacket);
         _timer.schedule( e , waitTime ) ;
         
         if( e._callback != null )return ;
         
       }
         
       synchronized( e ){
          outDebug("Waiting on Envelope");
          e.wait() ;   
          outDebug("Waiting on Envelope released");
       }
      
   }
   public class Envelope extends TimerTask {
   /* ---------------------------------------------------- */
       private long           _time ;
       private long           _messageId  = 0L ; 
       private boolean        _gotReply   = false ;
       private String         _reply      = null ;
       private String []      _payload    = null ;
       private ServerInfo     _serverInfo = null ;
       private boolean        _timeout    = false ;
       
       private RscmMessageArrivable _callback = null ;
       
       private Envelope( ServerInfo serverInfo ){
         _serverInfo = serverInfo ;
         _time       = System.currentTimeMillis() ;
       }
       public boolean isTimeout(){ return _timeout ; }
       public void setCallback( RscmMessageArrivable callback ){
          _callback = callback ;   
       }
       public RscmMessageArrivable getCallback(){
          return _callback ;   
       }
       public String [] getReplyVector(){ return _payload ; }
       public ServerInfo getServerInfo(){ return _serverInfo ; }
       public void setMessageId( long messageId ){
         _messageId = messageId ;
       }
       public String getPayload(){
         return "ping" ;
       }
       public String getMessage(){
          return  "rscm:2:"+_messageId+":r:"+getPayload() ;
          //return  "rscm" ;
       }
       public boolean gotReply(){ return _gotReply ; }
       public void replyArrived(){ }
       public void run(){
         synchronized( _requestMap ){
           outDebug("Removing ID, notifying listener");
           _timeout = true ;
           synchronized( this ){
              _requestMap.remove( _messageId ) ;
              if( _callback != null )_callback.messageArrived( this ) ;
              this.notifyAll() ;   
           }
         }
       }
       public void setReply( String message ){
           _gotReply = true ; 
           _reply    = message ;
           String [] x = _reply.split(":");
           int offset = 4 ;
           if( x.length < offset ){
              _payload = new String[0] ;
           }else{
              _payload = new String[x.length-offset] ;
              for( int i = 0 ; i < ( x.length-offset)  ; i++ )_payload[i] = x[i+offset] ;
           }
           
           replyArrived();
           outDebug("Removing ID, Cancelling timer and notifying listener");
           synchronized( this ){
              _requestMap.remove( _messageId ) ;
              this.cancel();
              this.notifyAll() ;   
           }
       }
       public String [] getResponsePayload(){return _payload ; }
   } 

   private class ServerInfo {
/* ---------------------------------------------------- */
      private int            _port         = 0 ;
      private InetAddress    _IPAddress    = null ;
      private String         _serviceName  = null ;
      private ServerInfo( String serverHostName , int port ) throws Exception {
          
         _IPAddress    = InetAddress.getByName( serverHostName );
         _port         = port ;
         
      }
   }
   public RscmClient() throws Exception {
/* ------------------------------------- */

       _clientSocket = new DatagramSocket();

       Thread listener = new Thread( new Listener() , "listener"  );
       listener.start();
       Thread housekeeping = new Thread(  new Housekeeping() , "housekeeping" );
       housekeeping.start();
       
   } 
   public void addServer( String serverHost , int port )throws Exception {
/* ----------------------------------------------------------------------- */
       synchronized( _serverList ){
           _serverList.add( new ServerInfo( serverHost , port ) ) ; 
       }
   }
   private class Listener  implements Runnable {

      public void run(){
          while(true){
             try{
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                outDebug("Waiting for incoming message, expecting : "+_requestMap.size());
                _clientSocket.receive(receivePacket);
                String message = new String( receivePacket.getData(),0,receivePacket.getLength() );
                outDebug("Received messsage : "+message);
                long messageId = getMessageIdFromMessage( message ) ;
                synchronized( _requestMap ){
                   outDebug("Searching for message id : "+messageId);
                   Envelope e = _requestMap.get( messageId );
                   if( e == null ){
                      outDebug("Received unwanted packet : "+message);
                      continue;
                   }
                   outDebug("Listener is setting Reply to : "+message);
                   e.setReply( message ) ;
                   
                   if( e._callback != null )e._callback.messageArrived( e ) ;
                }
             }catch(Exception ee ){
                outDebug("Receive loop interruped : "+ee );
                ee.printStackTrace();
                break ;
             }
          }

      }
   }
   /*
    *
    * Helper function. Get the messageId from the message string.
    */
   private long getMessageIdFromMessage( String message )throws IllegalArgumentException{
   /* ---------------------------------------------------------------------------------- */
      String [] x = message.split(":");
      if( x.length < 4 )throw new IllegalArgumentException("Not a valid packet : "+message);
      try{
         return Long.parseLong(x[3]);
      }catch(Exception ee ){
         throw new IllegalArgumentException( "Not a proper messageId : "+message); 
      }
   }
/* 
   =========================================================================== 
   =========================================================================== 
   =========================================================================== 
   =========================================================================== 
*/
   public static void main(String args[]) throws Exception {
       
     if( args.length < 3 ){
       System.err.println("Usage : [<hostname>:<portnumber> ...] device option");
       System.exit(0);
     }

     RscmClient client = new RscmClient();
     
     int i = 0 ;
     for(i = 0 ; i < args.length ; i++ ){
         
         String in = args[i] ;
         String [] ar = in.split(":") ;
         if( ar.length > 1 )
             client.addServer( ar[0] , Integer.parseInt( ar[1] ) ) ;
         else
             break ;
     }
     if( ( args.length - i ) > 0 ){
         if( ( args.length - i ) < 2 ){
           System.err.println("Usage : [<hostname>:<portnumber> ...] device option");
           System.exit(0);
         }
         String device    = args[i++] ;
         String option    = args[i++] ;
         
         while(true){
             option = option.equals("off") ? "on" : "off" ;
             outDebug("Sending "+device+" "+option);
             try{
                client.sendSetDeviceRequest( device , option , 10000L ) ;
             }catch(Exception eee ){
                outDebug("Exception in sendSetDeviceRequest "+eee) ; 
                eee.printStackTrace();
             }
             Thread.sleep(10000L);
        }
     }
   }
}
