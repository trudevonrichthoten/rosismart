package org.rosi.util ;

import java.io.* ;
import java.net.* ;

public class Forwarder {

   private int    _listenPort    = 0 ;
   private String _serverAddress = null ;
   private int    _serverPort    = 0 ;

   private ServerSocket _listenSocket = null ;
   
   public static void main( String [] args ) throws Exception {
       
      if( args.length < 3 ){
         System.out.println("Usage : ... <listenPort> <serverName> <serverPort>");
         System.exit(1);
      }
        Forwarder f = 
            new Forwarder( Integer.parseInt( args[0] ) ,
                           args[1] ,
                           Integer.parseInt( args[2] ) );
   }
   public Forwarder( int listenPort , String serverAddress , int serverPort )
   throws IOException {
       
       
      _listenPort = listenPort ;
      _serverAddress = serverAddress ;
      _serverPort    = serverPort ;
      
      _listenSocket = new ServerSocket( _listenPort ) ;
      
      new Thread( new ConnectionLauncher( _listenSocket ) ).start() ;
   }
   private class ConnectionLauncher implements Runnable {
       private ServerSocket _listen = null ;
       private ConnectionLauncher( ServerSocket listen ){
           _listen = listen ;
       }
       public void run(){
          while( true ){
              
             try{
                Socket socket = _listen.accept() ;
                System.out.println("Connection received by : "+socket);
                new ConnectionWorker( socket , _serverAddress , _serverPort ) ;
                
             }catch(Exception ee ){
                System.err.println("Listen reported : "+ee.getMessage() ) ;
                break ;
             }
             
          }
           
       }
   }
   private class ConnectionWorker {
       
        private Socket _socket1 = null , _socket2 = null ;
        
        private ConnectionWorker( Socket socket , String host , int port )
        throws IOException {
            
            _socket1 = socket ;
            _socket2 = new Socket( host , port ) ;
            
            System.out.println("Connected to : "+_socket2 ) ;
            
            InputStream  sock1in  = _socket1.getInputStream() ;
            OutputStream sock1out = _socket1.getOutputStream() ;
            
            InputStream  sock2in  = _socket2.getInputStream() ;
            OutputStream sock2out = _socket2.getOutputStream() ;
            
            new Thread( new CopyWorker( "(1) -> (2)" , sock1in , sock2out ) ).start() ;
            new Thread( new CopyWorker( "(2) -> (1)" , sock2in , sock1out ) ).start() ;
            new Thread( new Ping( sock2out ) ).start() ;
            
        }
   }
   private class Ping implements Runnable {
       private OutputStream _out = null ;
       private Ping( OutputStream out ){
          _out = out ;   
       }
       public void run(){
         try{
             while( true ){
                 Thread.currentThread().sleep(10000) ;
                 _out.write( '\n' ) ;
                 System.out.println("Ping");
             }
          }catch( Exception ee ){
             System.out.println("Ping Loop  got : "+ee ) ;
          }finally{
                try{ _out.close() ;}catch(Exception e1){}  ;
                System.err.println("Ping Loop terminated");      
          }
            
       }
   }
   private class CopyWorker implements Runnable {
       
       private InputStream  _in   = null ;
       private OutputStream _out  = null ;
       private String       _name = null ;
       private byte []      _buf  = new byte[1024] ;
       
       private CopyWorker( String name , InputStream in , OutputStream out ){
          _name = name ;
          _in   = in ;
          _out  = out ;         
       }
       public void run(){
              
          try{
             while( true ){
                 int rc = _in.read( _buf , 0 , _buf.length ) ;
                 if( rc <= 0 )break ;
                 printLine2( _buf , rc ) ;
                 _out.write( _buf , 0 , rc ) ;
                 System.out.println(_name+" Copied : "+rc+" bytes");
             }
          }catch( Exception ee ){
             System.out.println("I/O Loop "+_name+" got : "+ee ) ;
          }finally{
                try{ _in.close() ;}catch(Exception e1){} ;  
                try{ _out.close() ;}catch(Exception e1){}  ;
                System.err.println("Copy Loop "+_name+" terminated");      
          }
          
       }
       String [] cx = { "0" , "1" , "2" , "3" , "4" , 
                        "5" , "6" , "7" , "8" , "9" , 
                        "A" , "B" , "C" , "D" , "E" , "F"  } ;
                    
       public void printLine( byte [] buf , int len ){
           
          int cc = 0 ;
          
          for( int i = 0 ; i < len ; i++ ){
              
             int n = (int) buf[i] ;
             
             n = n >= 0 ? n : ( 16 + n ) ;
             
             String dig =  cx[ ( n >> 4 ) & 0xF ] + cx[n&0xF] ;
             
             char c = (char)buf[i] ;
             
             System.out.print(dig+"("+ ( Character.isLetterOrDigit(c) ? c : '.' ) +")" ) ;
             
             if( cc > 15 ){
                 System.out.println("");
                 cc = 0 ;
             }else{
                 System.out.print(" ");
                 cc ++ ;
             }
             
          }
          System.out.println("");
       }
       public void printLine2( byte [] buf , int len ){
           
          int cc = 0 ;
          char [] disp = new char[16] ;
          
          for( int i = 0 ; i < len ; i++ ){
              
             int n = (int) buf[i] ;
             
             n = n >= 0 ? n : ( 16 + n ) ;
             
             String dig =  cx[ ( n >> 4 ) & 0xF ] + cx[n&0xF] ;
             
             char   c = (char)buf[i] ;
             
             disp[cc] = Character.isLetterOrDigit(c) ? c : '.' ;
             
             System.out.print(dig) ;
             
             if( cc == 15 ){
                 
                 System.out.print(" *");
                 for( int j = 0 ; j < 16 ; j++ )System.out.print(disp[j]) ;
                 System.out.println("*");
                 cc = 0 ;
             }else{
                 System.out.print(" ");
                 cc ++ ;
             }


          }
          if( cc > 0 ){
             
             for( int z = cc ; z < 16 ; z++ )System.out.print("   ");
             System.out.print("*");
             for( int j = 0 ; j < cc ; j++ )System.out.print(disp[j]) ;
             for( int z = cc ; z < 16 ; z++ )System.out.print(" ");
             System.out.print("*");
          }
          System.out.println("");
       }
   }
    
}