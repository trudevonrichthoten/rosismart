package org.rosi.drivers.modem ;

import java.io.* ;
import gnu.io.* ;
import java.util.*;

public class HuwaiModemDriver extends ModemDriver {

  public HuwaiModemDriver( String portName ) throws Exception {

    super( portName ) ;

        /*
         * Switch to 'no echo'
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

        sendMessageToModem( "Hallo Dickes Schweinchen" );
        System.out.println( readResponse() ) ;
        try{ Thread.sleep(4000) ; }catch(Exception eee ){} ;
*/

  }
  public List<String> getResponseMessages() throws IOException {

    List<String> list = new ArrayList<String>() ;

    String str = null ;
    while( ( str = getResponseMessage() ) != null  )
       if( str.length() > 0 )
          list.add( str ) ;

    return list ;
  }
  public String getResponseMessage() throws IOException {

    String result = readString() ;

    return
        ( result == null ) ||
        ( result.compareToIgnoreCase("OK") == 0 ) ?
        null :
        result ;

  }

  public static void main( String[] args ) {
   
    if( args.length < 1 ){
       System.err.println("Usage : ... <device> [<command>]");
       System.exit(4);
    } 

    try {

      HuwaiModemDriver modem = new HuwaiModemDriver( args[0] ) ;

      modem.sendCommandToModem( args.length < 2 ? "AT" : args[1] ) ;
      try{ Thread.sleep(1000L) ; }catch(Exception ee ){}; 
      List<String> list = modem.getResponseMessages() ;
      for( String str : list ){
         System.out.println(str);
      }
  //    System.out.println( "Reponse : "+modem.readResponse() );

      modem.close();

   }catch( Exception e ) {

      e.printStackTrace();

    }
  }
}



