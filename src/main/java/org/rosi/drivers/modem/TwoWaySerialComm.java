package org.rosi.drivers.modem ;

import java.io.* ;
import gnu.io.* ;

public class TwoWaySerialComm {

  private InputStream _in ; 
  private OutputStream _out ; 
  private int [] _array = new int[2048] ;
  private int _arrayWriteCursor = 0 ;
  private int _arrayReadCursor  = 0 ;
  private int _arrayContent     = 0 ;
  void connect( String portName ) throws Exception {

    CommPortIdentifier portIdentifier 
         = CommPortIdentifier.getPortIdentifier( portName );

    if( portIdentifier.isCurrentlyOwned() ) {

      System.out.println( "Error: Port is currently in use" );

    } else {

      int timeout = 2000;

      CommPort commPort = portIdentifier.open( this.getClass().getName(), timeout );
 
      if( commPort instanceof SerialPort ) {

        SerialPort serialPort = ( SerialPort )commPort;

        serialPort.setSerialPortParams( /*57600*/ 115200 ,
                                        SerialPort.DATABITS_8,
                                        SerialPort.STOPBITS_1,
                                        SerialPort.PARITY_NONE );
 
        _in  = serialPort.getInputStream();
        _out = serialPort.getOutputStream();

        Thread reader = new Thread( new SerialReader( _in ) ) ;
        reader.start() ;
        /*
         * Switch to 'no echo'
         */      
        sendToModem( "ATE0") ;
        String reply = readResponse();
        System.out.println("Response ("+reply.length()+") : "+reply) ;

        sendToModem( "AT" ) ;
        reply = readResponse( );
        System.out.println("Response ("+reply.length()+") : "+reply) ;
        if( reply.compareToIgnoreCase("OK") != 0 )
          throw new
          IllegalArgumentException("Don't get proper return values from modem");

        sendToModem( "AT+CPIN=\"4474\"" ) ;
        System.out.println( readResponse() ) ;

        sendToModem( "AT+CGMM" ) ;
        System.out.println( readResponse() ) ;

        sendToModem( "AT+CMGF=1") ;
        System.out.println( readResponse() ) ;
        sendToModem( "AT+CMGS=\"+491707807476\"") ;
        System.out.println( readResponse() ) ;
        /*
         * The message
         */
        sendMessageToModem( "Hallo Dickes Schweinchen" );
        System.out.println( readResponse() ) ;
        try{ Thread.sleep(4000) ; }catch(Exception eee ){} ;
        reader.interrupt() ;
        _out.close();
        commPort.close();
      } else {

        System.out.println( "Error: Only serial ports are handled by this example." );

      }
    }
  }
  public void sendToModem( String message ) throws IOException{
     System.out.println("Sending : "+message);
     _out.write( message.getBytes() ) ; 
     _out.write(13);
  }
  public void sendMessageToModem( String message ) throws IOException{
    /*out.write(13) ;*/ 
     _out.write( "Hallo Dickes Schweinchen".getBytes() ) ; 
     _out.write(26) ;
  }
  public synchronized void add( int b )throws IOException{

     if( _arrayContent >= _array.length )
        throw new
        IOException("Input ring buffer overflow");

     _array[_arrayWriteCursor] = b ;
     _arrayWriteCursor  = ( _arrayWriteCursor + 1 ) % _array.length ;
     _arrayContent ++ ;
  }
  public synchronized int getNext(){
     if( _arrayContent == 0 )return -1 ;
     int out  = _array[_arrayReadCursor] ;
     _arrayReadCursor  = ( _arrayReadCursor + 1 ) % _array.length ;
     _arrayContent--;
     return out; 
  }
  public String readResponse() throws IOException {
    byte [] x = new byte[1024] ;
    int content = 0 ;

    try{ Thread.sleep(500) ; }catch(Exception eee ){};
    for( int i = 0 ; i < x.length ; i++ ){
 
      int n = getNext() ; 
   //   System.out.println(" int ; "+n+" char : "+(char)n);
      if( ( n == 10 ) || ( n == 13 ) )continue ;
      if( n < 0 ) return new String( x , 0 , content ) ;
      x[content++] = (byte)n ;
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

        while( ( ! Thread.interrupted() ) && ( ( n = this.in.read() ) > -1 ) )add(n); 

      }catch( Exception e ){

        e.printStackTrace();

      }
    }
  }
  public static void main( String[] args ) {
    try {

      TwoWaySerialComm twsc = new TwoWaySerialComm() ;

      twsc.connect( args[0] ) ;

    } catch( Exception e ) {
      e.printStackTrace();
    }
  }
}
