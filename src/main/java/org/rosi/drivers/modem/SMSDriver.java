package org.rosi.drivers.modem ;

import java.io.* ;
import gnu.io.* ;
import java.util.*;

public class SMSDriver extends BlockingModemDriver {


  private String _pin = null ;

  public SMSDriver( String portName ) throws Exception {

     super( portName ) ;

     _checkModem() ;

  }
  private void _checkModem() throws  IOException, InterruptedException {
     /*
      * no echo.
      */
     sendSimpleCommand( "ATE0" ) ;
     /*
      * get modem vendor.
      */
     String modemType = checkModem() ;

     if( modemType.compareToIgnoreCase( "huawei" ) != 0 )
       throw new
       IOException("Unknown modem found : "+modemType) ;
  }
  private void _loginModem( String pin ) throws IOException, InterruptedException {
     try{
        if( loginModem( pin )  ){
          /*
           * We really logged in now. Give the modem some time to adjust.
           */
          try{
            Thread.sleep(4000L) ;
          }catch(InterruptedException ie ){
            throw new
            IOException("Interrupted sleep after login");
          }
        }
     }catch(IOException ioe ){
        throw new
        IOException("Login failed ! ("+ioe.getMessage()+")");
     }
  }  
  public SMSDriver( String portName , String pin ) throws Exception {

     super( portName ) ;

     _checkModem();

     _loginModem( _pin = pin ) ;

  }
  public TextMessage [] listSMSs( String pin ) throws IOException , InterruptedException{

     _checkModem() ;

     _loginModem( pin ) ;

     sendSimpleCommand( "AT+CMGF=1") ;

     List<String> res = sendSimpleCommand( "AT+CMGL=\"ALL\"") ;

     List<TextMessage> messages = new ArrayList<TextMessage>() ;
     int state = 0 ;
     String [] header  = null ;
     StringBuffer body = null ;
     for( String s : res ){
        switch(state){

            case 0 :
              if( s.startsWith("+CMGL:") ){
                 header = s.split(",");
                 body   = new StringBuffer() ;
                 state  = 1 ;
              }else{
                 continue ;
              }
            break ;

            case 1 :
              if( s.startsWith("+CMGL:") || s.startsWith("OK") ){
                 String [] x = header[0].split(" ");      
                 int d = Integer.parseInt(x[1]);
                 boolean isNew = header[1].contains("UNREAD");
                 String  phone = header[2] ;
                 TextMessage message = new TextMessage( d , phone , header[1] ,  "unkown" ) ;
                 message.setBody( body.toString() );
                 messages.add( message ) ;

                 header = s.split(",");
                 body   = new StringBuffer() ;

              }else{
                 body.append(s) ;
              }
            break ;

        }
     }

     return messages.toArray( new TextMessage[0] ) ;

  }
  public TextMessage [] fetchSMSs() throws IOException , InterruptedException {
      if( _pin == null )
        throw
        new IOException("PIN not specified");

      return fetchSMSs( _pin ) ;
  }
  public TextMessage [] fetchSMSs( String pin ) throws IOException , InterruptedException {

     TextMessage [] messages = listSMSs( pin ) ;

     ArrayList<TextMessage> list = new ArrayList<TextMessage>() ;

     for( int i = 0 ; i < messages.length ; i++ ){

         TextMessage msg = messages[i] ;

         if( msg.getMode().contains("UNREAD") ){
             list.add(msg);
             deleteSMS( pin , msg.getId() ) ;
         }
     }

     return list.toArray( new TextMessage[0] ) ;
  }
  public void clearSMSs( String pin ) throws IOException , InterruptedException {

     TextMessage [] messages = listSMSs( pin ) ;

     ArrayList<TextMessage> list = new ArrayList<TextMessage>() ;

     for( int i = 0 ; i < messages.length ; i++ ){

         TextMessage msg = messages[i] ;
         deleteSMS( pin , msg.getId() ) ;
     }
     return ;
  }
  public void deleteSMS( String pin ,  int messageID ) throws IOException , InterruptedException {

     _checkModem() ;

     _loginModem( pin ) ;

     sendSimpleCommand( "AT+CMGD="+messageID) ;
  }
  public void sendSMS( String pin ,  String phoneNumber , String message ) 
     throws IOException, 
            InterruptedException {

     _checkModem() ;

     _loginModem( pin ) ;

     sendSimpleCommand( "AT+CMGF=1") ;
     sendSimpleCommand( "AT+CMGS=\""+phoneNumber+"\"") ;
     sendSimpleMessage( message );
  }
  public static void main( String[] args ) {
   
    if( args.length < 1 ){
       System.err.println("Usage : ... <device> <pin> [<command>]");
       System.err.println("     <command> : ");
       System.err.println("         send <phoneNumber> <message>");
       System.err.println("         list");
       System.err.println("         fetch");
       System.err.println("         fetchloop");
       System.err.println("         delete <messageId>");
       System.exit(4);
    } 

    try{

       String portName = args[0] ;

       SMSDriver modem = new SMSDriver( portName ) ;

       try {

           if( args.length < 2 ){
              String infoString = modem.getModemInfo() ;
              System.out.println("Modem : "+infoString);
           }else if( args[2].equals( "list" ) ){
              String pin = args[1] ;
              TextMessage [] messages = modem.listSMSs(pin);
              for( int i = 0 ; i < messages.length ; i++ ){
                System.out.println(messages[i].toString());
              }
           }else if( args[2].equals( "fetch" ) ){
              String pin = args[1] ;
              TextMessage [] messages = modem.fetchSMSs(pin);
              for( int i = 0 ; i < messages.length ; i++ ){
                System.out.println(messages[i].toString());
              }
           }else if( args[2].equals( "fetchloop" ) ){
              String pin = args[1] ;
              while( true ){
                 TextMessage [] messages = modem.fetchSMSs(pin);
                 for( int i = 0 ; i < messages.length ; i++ ){
                   System.out.println(messages[i].toString());
                 }
                 Thread.sleep(10000L);
              }
           }else if( args[2].equals( "send2" ) ){
              if( args.length < 5 ){
                  System.err.println( "Usage : ... <device> <pin> send <phoneNumber> <message>");    
              }else{
                 String pin         = args[1] ;
                 String phoneNumber = args[3] ;
                 String message     = args[4] ;
                 for( int i = 0 ; i < 3 ; i++  ){
                    modem.sendSMS( pin , phoneNumber , message ) ;
                 }
              }
           }else if( args[2].equals( "send" ) ){
              if( args.length < 5 ){
                  System.err.println( "Usage : ... <device> <pin> send <phoneNumber> <message>");    
              }else{
                 String pin         = args[1] ;
                 String phoneNumber = args[3] ;
                 String message     = args[4] ;
                 modem.sendSMS( pin , phoneNumber , message ) ;
              }
           }else if( args[2].equals( "delete" ) ){
              if( args.length < 4 ){
                  System.err.println( "Usage : ... <device> <pin> delete <messageID>");    
              }else{
                 String pin             = args[1] ;
                 String messageIdString = args[3] ;
                 int messageID          = Integer.parseInt( messageIdString ) ;
                 
                 modem.deleteSMS( pin , messageID ) ;
              }
           }

       }catch( Exception e ) {

           System.err.println("Problem in command : "+e.getMessage() ) ;
           e.printStackTrace();

       }finally{
           modem.close() ;
       }

    }catch(Exception oe ){
       System.err.println("Problem in command : "+oe.getMessage() ) ;
       oe.printStackTrace();
    }
  }
  
}
