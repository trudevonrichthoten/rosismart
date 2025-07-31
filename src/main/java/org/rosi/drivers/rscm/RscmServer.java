package org.rosi.drivers.rscm;

import java.io.*;
import java.net.*;
import java.util.*;

class RscmServer {

  //
  //  Return codes
  //  1 : 0 length message received.
  //  2 : 0 length message received.
  //  3 : Not a 'rscm' message. 
  //  4 : No version number found.
  //  5 : Version not supported.
  //  6 : No message ID.
  //  7 : Not enough arguments.
  //  8 : Command not supported.
  //
  private int    _portNumber  = 0 ;
  private String _serviceName = null ;
  
  public RscmServer( String serviceName, int portNumber ) throws Exception {
      _serviceName = serviceName ;
      _portNumber  = portNumber ;
      
      Server server = new Server( portNumber ) ;
      Thread serverThread = new Thread( server )  ;
      serverThread.start() ;
  }
  public class Server implements Runnable {
      
      private DatagramSocket _clientSocket = null ;     
      private byte[] _receiveData = new byte[4048];
      
      private Server( int portNumber ) throws Exception {
          _portNumber   = portNumber ;
          _clientSocket = new DatagramSocket(portNumber);
      }
      public void run() {
         try{
             runLoop();
         }catch(Exception ee ){
            System.err.println("Exception in receive loop : "+ee ) ;   
         }finally{
             try{
                 _clientSocket.close();
             }catch(Exception eee ){
                 
             }
         }
      }
      public void runLoop() throws Exception {
          
          while( ! Thread.interrupted() ){
    
              DatagramPacket receivePacket = new DatagramPacket(_receiveData, _receiveData.length);
              _clientSocket.receive(receivePacket);
              
              String request = new String(receivePacket.getData(),0,receivePacket.getLength());

              String reply = processRequest(request); 
              
              if( reply == null )continue ;

              byte [] sendData   = reply.getBytes();
              DatagramPacket sendPacket = 
                   new DatagramPacket(
                          sendData, 
                          sendData.length, 
                          receivePacket.getAddress(), 
                          receivePacket.getPort()     );
              _clientSocket.send(sendPacket);
    
          }
          
      }
   }
   public  String processRequest( String in ){
       
      //
      //   rscm:<version>:<msgId>:[r|x]:
      //
      String [] x = in.split(":");
      
      if( x.length < 4 )return null ;
      
      try{
      
          String protocolType   = x[0] ;
          int  protocolVersion  = Integer.parseInt(x[1]) ;
          String messageId      = x[2] ;
          
          if( ! protocolType.equals("rscm" ) )return null ;

          if( protocolVersion != 2 ){
             return   protocolType+":"+protocolVersion+":x:"+messageId+":2:Version not supported" ;
          }
          
          if( x.length < 5 ){
             return   protocolType+":"+protocolVersion+":x:"+messageId+":2:No command found." ;
          }
          String command = x[4] ;
          
          if( command.equals("ping") ){
             return   protocolType+":"+protocolVersion+":x:"+messageId+":0:"+_serviceName ;
          }else if( command.equals("getdevicenames") ){
             StringBuffer sb = new StringBuffer() ;
             sb.append(protocolType).append(":").append(protocolVersion).
                append(":x:").append(messageId).append(":0:#BL:") ;
             sb.append(_serviceName).append(".").append("trude1");
             sb.append(":") ;
             sb.append(_serviceName).append(".").append("trude2");
             sb.append(":") ;
             sb.append(_serviceName).append(".").append("trude3");
             sb.append(":") ;
             sb.append("#EL");
             return sb.toString();
          }else if( command.equals("setdevice") ){
             if( x.length < 7 ){
                return   protocolType+":"+protocolVersion+":x:"+messageId+":2:Command not found." ;
             }
             String device = x[5] ;
             String option = x[6] ;
             System.out.println("Set Device : "+device+" to "+option);
             if( device.equals( _serviceName+".trude1" ) ){
                 return   protocolType+":"+protocolVersion+":x:"+messageId+":0:ok" ;
             }else if( device.equals( _serviceName+".trude2") ){
                 try{
                     Thread.sleep(5000L);
                 }catch(Exception ee ){
                 }
                 return   protocolType+":"+protocolVersion+":x:"+messageId+":0:ok" ;
             }else if( device.equals( _serviceName+".trude3") ){
                 return null ;
             }else{
                 return   protocolType+":"+protocolVersion+":x:"+messageId+":3:Command not found." ;
             }
          }else{
             return   protocolType+":"+protocolVersion+":x:"+messageId+":4:Command not supported." ;
          }
          
      }catch(Exception ee ){
          System.out.println("Exception in "+in+" : "+ee);
          return null ;
      }
   }
   public static void main(String args[]) throws Exception {
       
     if( args.length < 2 ){
       System.err.println("Usage : <serviceName> <portnumber>");
       System.exit(0);
     }
     String serviceName = args[0] ;
     int    portNumber  = Integer.parseInt( args[1] ) ;

     RscmServer server = new RscmServer( serviceName , portNumber )  ;
   }
   
}
