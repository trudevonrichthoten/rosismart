package org.rosi.drivers.nut ;

import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;

import org.rosi.drivers.generic.XGenericDriver ;


public class NutUpsDriver implements XGenericDriver {

   private static      int UPSD_PORT =  3493 ;
   private PrintWriter    _writer   = null ;
   private BufferedReader _reader   = null ;
   private int            _port     = 0 ;
   private String         _hostname = null ;
   private Socket         _socket   = null ;
 
   private List<String>   _deviceList = new ArrayList<>() ;

   public static String  _patternString = "VAR[ ]*([a-zA-Z0-9]*)[ ]*([a-zA-Z\\.0-9]*)[ ]*\"(.*)\"" ;
   public static Pattern _pattern       = Pattern.compile( _patternString ) ;

   public NutUpsDriver( String hostname , int port ) throws IOException {
        _hostname = hostname ;
        _port     = port ;
   }
   public void addDeviceName( String deviceName ){
        _deviceList.add(deviceName);
   }
   private void connect() throws IOException {

       _socket = new Socket( _hostname , _port ) ;

       try{

          _writer = new PrintWriter(  new OutputStreamWriter( _socket.getOutputStream() ) ) ;

          try{

             _reader = new BufferedReader(  new InputStreamReader( _socket.getInputStream() ) ) ;

          }catch( IOException e){
             try{ _writer.close() ; _writer = null ; }catch(Exception eee ){}
             throw e ;
          }

       }catch( IOException ee ){
          try{ _socket.close() ; _socket = null ; }catch(Exception eee ){}
          throw ee ;
       }
   }
   public void update() throws Exception {

   }
   public void setDeviceAttribute( String deviceName , String key , String value ) throws Exception {
      throw new Exception("'setDeviceAttribute' not supported by this device!");
   }
   public List<String> getDeviceNames() throws Exception {
       return  _deviceList ;
   }
   public Map<String,String> getDeviceAttributes( String device ) throws Exception {
       return getVariableList( device ) ;
   }
   public Map<String,String> getVariableList( String device ) throws IOException {

     String requestString = "list var "+device;

     connect();

     try{
     
        _writer.println( requestString ) ;
        _writer.flush();

        int status = checkReplyOk( _reader.readLine() ) ;
        if( status != 0 )
          throw new
          IOException("Reply sequence confused : expected '0' got : "+status);

        Map<String,String> map = new HashMap<String,String>() ;
        while( true  ){

           String s = _reader.readLine() ;

           if( checkReplyOk( s ) != 1 )break ; 

           Matcher m = _pattern.matcher( s );

           if( ! m.matches() )
              throw new
              IOException("Unexpected syntax in reply from 'upsd' : >"+s+"<");

           map.put( m.group(2) , m.group(3) ) ;

        }

        return map;

     }finally{
        close();
     }

   }

   private int checkReplyOk( String message ) throws IOException {

     if( message == null ) 
       throw new
       IOException("Premature end of information");

     String [] tokens = message.split(" ") ;

     if( tokens.length == 0 )
       throw new
       IOException("Unexpected empty line from 'upsd'");
       
     if( tokens[0].compareToIgnoreCase("err") == 0 )
       throw new
       IOException("Error reported from 'upsd' : "+tokens[0]);  
    
     if( tokens.length < 4 )
       throw new
       IOException("List didn't start with proper message <"+message+">");
 
     if( tokens[0].compareToIgnoreCase("begin") == 0 )return 0; 
     if( tokens[0].compareToIgnoreCase("end") == 0 )return 2; 
     if( tokens[0].compareToIgnoreCase("var") == 0 )return 1; 

     throw new
     IOException("Unexpected keyword in reply from upsd : "+tokens[0]);
   }
   public void close() throws IOException {

      try{

         _writer.println( "logout" ) ;
         _writer.flush();

         while( _reader.readLine() != null );

      }catch(Exception ee ){
         try{ _writer.close() ; _writer = null ; }catch(Exception eee ){}
         try{ _reader.close() ; _reader = null ; }catch(Exception eee ){}
         try{ _socket.close() ; _socket = null ; }catch(Exception eee ){}
      }
   }
   public static void main( String [] args ) throws Exception {
/*
        Matcher m = _pattern.matcher(args[0]);
        boolean b = m.matches();
        System.out.println("result : "+b+" count "+m.groupCount());

        for( int i = 1 ; i <= m.groupCount() ; i++ ){
            System.out.println("  "+i+" "+m.group(i));
        }
*/

      if( args.length < 1 ){
         System.err.println(" Usage : <...> <Nuts Host> [<Nuts Device Name> ...]");
         System.exit(1);
      }
      NutUpsDriver ups = new NutUpsDriver( args[0] ,  UPSD_PORT ) ;
      try{
         for( int i = 1 ; i < args.length ; i++ )ups.addDeviceName( args[i] ) ;

         for( String deviceName : ups.getDeviceNames() ){
             System.out.println(" -- "+deviceName);
         
             for( Map.Entry<String,String> e : ups.getDeviceAttributes(deviceName).entrySet() ){
                 System.out.println( "  "+e.getKey() + " -> "+e.getValue() );
             }
         }
      }finally{
      }
/*
      System.out.println("----");
      Map<String,String> map = ups.getVariableList("aeg") ;
      System.out.println("Result ; "+map);
      while( true ){
         System.out.println("----");
         map = ups.getVariableList("aeg") ;
         for( Map.Entry e : map.entrySet() ){
           System.out.println("  "+e.getKey()+ " -> "+e.getValue() ) ;
         }
         Thread.sleep(5000L);
      }
      //ups.close() ;
*/
   }
}
