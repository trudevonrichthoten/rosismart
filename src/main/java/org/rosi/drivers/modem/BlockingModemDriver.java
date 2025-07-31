package org.rosi.drivers.modem ;

import java.io.* ;
import gnu.io.* ;
import java.util.*;
import java.util.concurrent.BlockingQueue ;
import java.util.concurrent.ArrayBlockingQueue ;
import java.util.concurrent.TimeUnit ;

public class BlockingModemDriver {

  private InputStream  _in ; 
  private OutputStream _out ; 
  private SerialPort   _serialPort ;
  private CommPort     _commPort ;
  private Thread       _readerThread ;

  private BlockingQueue<Integer> _queue = new ArrayBlockingQueue<Integer>(2048);

  private boolean _dontTryAgain = false ;

  private boolean _debug = true ;

  private void log(String message ){ System.out.println("Modem: "+message) ; }

  public BlockingModemDriver( String portName , String simPin ) throws Exception {
     this(portName) ;
     /*
      * no echo. 
      */
     sendSimpleCommand( "ATE0" ) ; /* no echo */
     /*
      * get modem vendor. 
      */
     String modemType = checkModem() ;
     if( modemType.compareToIgnoreCase( "huawei" ) != 0 )
       throw new
       IOException("Unknown modem found : "+modemType) ; 

     loginModem( simPin ) ;

  }
  public BlockingModemDriver( String portName ) throws Exception {


    CommPortIdentifier portIdentifier  = null ;
   
    try{
       portIdentifier = CommPortIdentifier.getPortIdentifier( portName );
    }catch(NoSuchPortException nspe ){
       throw new
       IOException("No such port : "+portName);
    }

    if( portIdentifier.isCurrentlyOwned() )
       throw new
       IOException("Device is owned by someone else");


     int timeout = 2000;

     _commPort = portIdentifier.open( this.getClass().getName(), timeout );
 
     if( ! ( _commPort instanceof SerialPort ) )
       throw new
       IOException("Not a serial port : "+portName);

     _serialPort = ( SerialPort )_commPort;

     _serialPort.setSerialPortParams( /*57600*/ 115200 ,
                                   SerialPort.DATABITS_8,
                                   SerialPort.STOPBITS_1,
                                   SerialPort.PARITY_NONE );
 
     _in  = _serialPort.getInputStream();
     _out = _serialPort.getOutputStream();

     _readerThread = new Thread( new SerialReader( _in ) , "USBttyReader") ;
     _readerThread.start() ;

  }
  public void close() throws Exception {
/*----------------------------------------------------------*/

    _readerThread.interrupt() ;

     _out.write( "at".getBytes() ) ; _out.write(13);

    _out.flush();
    _out.close();
    _in.close();
    _commPort.close();

  } 
  public void sendCommandToModem( String message ) throws IOException{
/*----------------------------------------------------------*/

     if( _debug )log(" DEBUG : sending command : "+message ) ;

     _out.write( message.getBytes() ) ; 
     _out.write(13);

  }
  public void sendMessageToModem( String message ) throws IOException{
/*----------------------------------------------------------*/
     if( _debug )log(" DEBUG : sending message : "+message ) ;

     _out.write( message.getBytes() ) ; 
     _out.write(26) ;

  }
  public String getModemInfo() throws IOException,InterruptedException {
/*----------------------------------------------------------*/

      StringBuffer sb = new StringBuffer() ;

      sb.append( sendSimpleCommand("AT+CGMI")).append(",") ;
      sb.append( sendSimpleCommand("AT+CGMM")).append(",") ;
      sb.append( sendSimpleCommand("AT+CGMR")).append(",") ;
      sb.append( sendSimpleCommand("AT+CGSN"));

      return sb.toString() ;
  }
  /** 
    * sendSimpleMessage.
    * ------------------
    */
  public void sendSimpleMessage( String message ) throws IOException,InterruptedException {
     sendSimpleMessage( message , 10000L ) ;
  }
  /** 
    * sendSimpleMessage.
    * ------------------
    */
  public void sendSimpleMessage( String message , long waitTime ) 
/*----------------------------------------------------------*/
     throws IOException,InterruptedException {

     List<String> l = null ;

     for( int i = 0 ; true ; i++ ){

        try{
           sendMessageToModem( message ) ;     

           l = getResponseMessages( waitTime ) ;
 
           break ;

        }catch(IOException ioe ){
           if( i > 3 ){
              log( "Timeout in getResponseMessage ("+message+") : giving up");
              throw ioe ;
           }
           log( "Timeout in getResponseMessage ("+message+") : trying again ("+i+")");
        }
     }

     if( (l.size() > 0 ) && ( l.get(l.size()-1).compareToIgnoreCase("OK") == 0 ) )return ;

     StringBuffer errorMsg = new StringBuffer() ;
     for( String s : l )errorMsg.append(s).append(";");

     throw 
      new IOException("Error from Modem after sending message : "+errorMsg.toString()); 
  }
  public List<String> sendSimpleCommand( String command ) 
/*----------------------------------------------------------*/
     throws IOException,
            InterruptedException {
     return sendSimpleCommand( command , 10000L ) ;
  }
  public List<String> sendSimpleCommand( String command , long waitTime ) 
/*----------------------------------------------------------*/
     throws IOException,
            InterruptedException {

     List<String> l = null ;


     for( int i = 0 ; true ; i++ ){

        try{

           sendCommandToModem( command ) ;     

           l = getResponseMessages(waitTime) ;

           if( l.size() == 0 )
             throw new
             IOException("No proper answer from modem" ) ;

           if( l.size() == 1 ){

             if( ( l.get(0).compareToIgnoreCase("OK") == 0 )  ||
                 ( l.get(0).compareToIgnoreCase(">")  == 0  )     )return l ;

             throw 
               new IOException("Problem reported : "+l.get(0) ) ;
 
           }
 
           break ;

        }catch( IOException ioe ){
           if( i > 3 ){
              log( "Timeout in getResponseMessage for ("+command+") : giving up");
              throw ioe ;
           }
           log( "Timeout in getResponseMessage for ("+command+") : trying again ("+i+")");
        }

     }

     if( l.get(l.size()-1).compareToIgnoreCase("ok") == 0 )return l ;

     StringBuffer errorMsg = new StringBuffer() ;
     for( String s : l )errorMsg.append(s).append(";");

     throw 
       new IOException("Error from Modem after sending message : "+errorMsg.toString()); 

  }
  public List<String> getResponseMessages( long timeout ) throws IOException, InterruptedException {

    List<String> list = new ArrayList<String>() ;

    String str = null ;
    while( true ){
       str = readString( timeout ).trim() ;
       if( _debug )log(" DEBUG : readString : "+str);
       if( str.length() > 0 )list.add( str ) ;
       if( str.equals("OK") || str.equals(">")  )break ;
    }

    return list ;
  }
  public String readString() throws IOException, InterruptedException {  return readString(10000L) ; }
  public String readString( long milliSec ) 
    throws IOException, 
           InterruptedException {

    byte [] x = new byte[4*1024] ;
    int content = 0 ;

    for( int i = 0 ; i < x.length ; i++ ){

      int n = 0 ; 
      Integer ni  = _queue.poll( milliSec , TimeUnit.MILLISECONDS ) ; 
      if( ni == null ){
         if( _debug )log(" DEBUG: Timeout from _queue.poll");
         throw
            new IOException("Reply timed out");
      }else{
         n = (int)ni ;
      }

      if( _debug )log(" DEBUG: int = "+n+" char : "+(char)n);

      if( n == 13 )continue ; 

      if( ( n == 10 ) || ( ( n == 32 ) && ( content == 1 ) && ( x[content-1] == 62 ) ) ){
         String result = new String( x , 0 , content ) ;
         if( _debug )log(" DEBUG : readString : returning : >"+result+"<");
         return result ;
      }

      x[content++] = (byte)n ;

    }

    for( int i = 0 ; i < x.length ; i++ ){
      log(" PANIC: int = "+x[i]+" char : "+(char)x[i]);
    }
    throw new 
    IOException("Internal Error : Too many input bytes");

  }
  private String readResponse(long timeout) throws IOException, InterruptedException {

    byte [] x = new byte[1024] ;
    int content = 0 ;

    try{ Thread.sleep(500) ; }catch(Exception eee ){};

    int n ;
    while( ( ( n = _queue.poll( timeout , TimeUnit.MILLISECONDS ) ) == 10 ) || ( n == 13 ) );

    for( int i = 0 ; i < x.length ; i++ ){
 
    //  System.out.println(" int ; "+n+" char : "+(char)n);

      if( n < 0 ) return new String( x , 0 , content ) ;

      if( n == 10 )continue ; 
      if( n == 13 )n = '\n' ;

      x[content++] = (byte)n ;

      n = _queue.poll( timeout , TimeUnit.MILLISECONDS) ; 
    }
    throw new IOException("Too many input bytes");
  } 
  public class SerialReader implements Runnable {
 
    InputStream in;
 
    public SerialReader( InputStream in ) {
      this.in = in;
    }
 
    public void run() {
      byte[] buffer = new byte[ 1024 ];
      int n   = 0; 
      try {

        while( ( ! Thread.interrupted() ) && ( ( n = this.in.read() ) > -1 ) ){
          if(_debug)log(" DEBUG : Adding : "+n);
          _queue.add(n); 
        }

      }catch( Exception e ){

        e.printStackTrace();

      }
      //System.err.println(" reader thread finished");
    }
  }
  public String checkModem() throws IOException,InterruptedException {

      List<String> s = sendSimpleCommand("AT") ;
      if( s.get(s.size()-1).compareToIgnoreCase("ok") != 0 ){
        /*
         * Problem , lets try again (just once).
         */
          s = sendSimpleCommand("AT") ;
          if( s.get(s.size()-1).compareToIgnoreCase("ok") != 0 )
            throw new
            IOException("Problem sending 'at' commands");
      }
      s = sendSimpleCommand("AT+CGMI")  ;
      if( s.get(s.size()-1).compareToIgnoreCase("ok") != 0 )
         throw new
         IOException("Can't get modem vendor");

      return s.get(0) ;
  }
  public boolean loginModem( String password )throws IOException,InterruptedException {

     List<String> msg = sendSimpleCommand( "AT+CPIN?" ) ;
     if( msg.size() == 0  )
           throw new
           IOException("Login: No reply from 'Login Request'");

     if( ( msg.size() == 1 ) || ( msg.get(msg.size()-1).compareToIgnoreCase("ok") != 0 ) )
           throw new
           IOException("Login failed "+msg.get(msg.size()-1));

     String login = msg.get(0);

     if( login.compareToIgnoreCase("+CPIN: SIM PIN" ) == 0 ){

        //System.out.println("Login : "+login);
        if( _dontTryAgain )
           throw new
           IOException("Wrong password (won't retry login)");
        try{
           String loginInfo = "AT+CPIN=\""+password+"\"" ;
           System.out.println("Sending login : "+loginInfo);
           sendSimpleCommand(loginInfo); 
        }catch( IOException ioe ){
           _dontTryAgain = true ;
           throw ioe ;
        }

        return  true ;
     }else if( login.compareToIgnoreCase("+CPIN: READY") == 0 ){
        return false ;
     }else{
        _dontTryAgain = true ;
        throw new
        IOException("Login failed : "+login);
     }
  }
  public static String [] __command = {
      "AT+CGMI" ,
      "AT+CGMM" ,
      "AT+CGMR" ,
      "AT+CGSN" ,
      "AT+CPIN?" , 
      "AT+CIMI" ,
      "AT+CREG?" ,
      "AT+COPS?" , 
      "AT+CSQ"
  };
  public static void main( String[] args ) throws Exception {
   
    if( args.length < 1 ){
       System.err.println("Usage : ... <device> [<command>]");
       System.exit(4);
    } 

    BlockingModemDriver modem = new BlockingModemDriver( args[0] ) ;
    
    try {
      
      if( args.length == 1 ){
         String result ;
         for( int i = 0 ; i < __command.length ; i++ ){
            System.out.print(__command[i] + " -> " ) ;
            System.out.println(modem.sendSimpleCommand( __command[i] ));
         } 
      }else{
  
         if( args[1].equals("send" ) ){
            if( args.length < 4 ){
               System.out.println("<dev> send <phoneNumber> <message>");
               System.exit(4);
            }
            String phoneNumber = args[2] ;
            String message     = args[3] ;
            String result ;
            modem.sendCommandToModem( "AT+CMGF=1") ;
            try{ Thread.sleep(200);}catch(Exception eee ){};
            while( ( result = modem.readString() ) != null ){
              if(  result.trim().length() == 0  )continue; 
              System.out.println(result ) ;
            }
            modem.sendCommandToModem( "AT+CMGS=\""+phoneNumber+"\"") ;
            try{ Thread.sleep(500);}catch(Exception eee ){};
            while( ( result = modem.readString() ) != null ){
              if(  result.trim().length() == 0  )continue; 
              System.out.println(result ) ;
            }
            modem.sendMessageToModem( message );
            while( ( result = modem.readString() ) != null ){
              //if( ( result.length() == 0 ) || result.equals("OK" ) )continue; 
              System.out.println(result ) ;
            }

         }else if( args[1].equals("check" ) ){
         }else if( args[1].equals("xlogin" ) ){
            if( args.length < 3 ){
               System.out.println("Usage : <device> xlogin <password>");
            }else{
               try{
                  modem.loginModem( args[2] ) ;
               }catch(Exception eeee ){
                  System.out.println("Exception : "+eeee);
               }
           }
/*
         }else if( args[1].equals("login" ) ){
            String login = modem.sendSimpleCommand( "AT+CPIN?" ) ;
            if( login.compareToIgnoreCase("+CPIN: SIM PIN" ) == 0 ){
               if( args.length < 3 ){
                  System.out.println("Need to log in");
               }else{
                  login = modem.sendSimpleCommand("AT+CPIN=\""+args[2]+"\""); 
                  System.out.println("Result from login : "+login);
               } 
            }else if( login.compareToIgnoreCase("+CPIN: READY") == 0 ){
               System.out.println("Already logged in");
            }else{
               System.out.println("Unknown reply from Modem : "+login);
            }
*/
         }else if( args[1].equals("info" ) ){
            StringBuffer sb = new StringBuffer() ;
            sb.append( modem.sendSimpleCommand("AT+CGMI")).append(",") ;
            sb.append( modem.sendSimpleCommand("AT+CGMM")).append(",") ;
            sb.append( modem.sendSimpleCommand("AT+CGMR")).append(",") ;
            sb.append( modem.sendSimpleCommand("AT+CGSN"));
            System.out.println( sb.toString() );
         }else if( args[1].equals("list" ) ){
            modem.sendCommandToModem( "AT+CMGF=1") ;
            modem.sendCommandToModem( "AT+CMGL=\"ALL\"") ;
            try{ Thread.sleep(1000);}catch(Exception eee ){};
            String result ;
            while( ( result = modem.readString() ) != null ){
              //if( ( result.length() == 0 ) || result.equals("OK" ) )continue; 
              System.out.println(result ) ;
            }
         }else{
            List<String> res =  modem.sendSimpleCommand( args.length < 2 ? "AT" : args[1] ) ;
            System.out.println(res);
         }
      }

   }catch( Exception e ) {

      e.printStackTrace();

   }finally{

      modem.close();

   }
  }
}



