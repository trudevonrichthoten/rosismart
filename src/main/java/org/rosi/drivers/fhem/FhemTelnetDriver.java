package org.rosi.drivers.fhem ;

import java.io.* ;
import java.net.* ;

public class FhemTelnetDriver implements Runnable {

   private String  _hostname  = null ;
   private int     _port      =  0 ;
   private boolean _connected = false ;
   private Socket  _socket    = null ;

   private PrintWriter    _out  = null ;
   private BufferedReader _in   = null ;

   public FhemTelnetDriver( String hostname , int port ) throws Exception {

      _hostname  = hostname ;
      _port      = port ;
      _connected = false ;

   }
   public void go(){
      new Thread(this).start() ;
   }
   private void errorLog( String message ){
   //   System.err.println(message);
   }
   public void run(){

      while(true){
          try{
               Thread.sleep(10000L);

               sendCommand("Hallo Trude");

          }catch(Exception ee ){
               errorLog("Got exception in send ... continuing");
          }
      }      
   }
   public void sendCommand( String command ) throws Exception {
      _connect() ;  
      _out.println(command);
      _out.flush();
      errorLog("Send command : "+command);
   }
   public class InputDumpster implements Runnable {

       public void run(){
          char [] buffer = new char[1024] ; 
          try{
              while(true){
                 int rc = _in.read(buffer) ;
                 if( rc < 0 )break ;
                 errorLog("received "+rc+" chars from network");              
              }
          }catch( Exception ee ){
              errorLog("Got an exception from input stream : "+ee); 
          }
          errorLog("InputDumpster finished");
          _close();
       }
   }
   private synchronized void _close() {

      try{ _out.close() ; }catch(Exception ee ){}
      try{ _in.close() ; }catch(Exception ee ){}
      try{ _socket.close() ; }catch(Exception ee ){}
      _connected = false ;
   }
   private void _connect() throws Exception {

//      while( true ){
          synchronized(this){
             if( _connected )return ;
             try{
                 errorLog("Trying to connect");
                 _socket = new Socket( _hostname , _port ) ;
                 _out = new PrintWriter( new OutputStreamWriter( _socket.getOutputStream() ) ) ;
                 _in  = new BufferedReader( new InputStreamReader( _socket.getInputStream() ) );
                 new Thread( new  InputDumpster() , "InputDumpster").start() ;
                 errorLog("Connection established");
                 _connected = true ;
             }catch( Exception ee ){
                 _connected = false ;
                 errorLog( "Exception in connecting to "+_hostname+":"+_port+" : "+ee);
                // if( ee instanceof UnknownHostException )throw ee ;
                 throw ee ;
                 
             }
          }
//          Thread.sleep(10000L);
//      }

   }
   public static void main( String [] args ) throws Exception {

      if( args.length < 3 ){     
          System.err.println("Usage : ... <host> <port> command ...");
          System.exit(4);
      }
      String hostname = args[0] ;
      int    port     = Integer.parseInt( args[1] ) ;
      String command  = args[2] ;

      FhemTelnetDriver driver = new FhemTelnetDriver( hostname , port ) ;
      driver.go();

   }

}
