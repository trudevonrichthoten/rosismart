package org.rosi.drivers.telnet ;

import java.net.* ;
import java.io.* ;
import java.util.* ;


public class TelnetDriver {


   private PrintWriter _pw = null ;
   private BufferedReader _br = null ;
   
   public TelnetDriver( String hostname , int port ) throws Exception {

     _hostname = hostname  ;
     _port     = port ;

/*
       Socket             s   = new Socket( hostname , port );
       TelnetStreamEngine tse = new TelnetStreamEngine( s , null ) ;

       _pw = new PrintWriter( tse.getWriter() ) ;

       _br = new BufferedReader( tse.getReader() ) ;

       System.out.println(" BufferedReader ok ");
      

       new Thread( 
         new Runnable(){
            public void run(){

                String in = null ; 
                try{
                   while( ( in = _br.readLine() ) != null ){
                      System.out.println(" -> "+in );
                   }
                }catch(Exception ee ){
                   System.err.println("Exeption in loop  : "+ee );
                }
 
            }
         }
       ).start() ;
*/

   }   
   private int    _port     = 0 ;
   private String _hostname = null ;

   public void setCredentials( String user , String passwd ){

   }
   public void authenticate() throws Exception {

   }
   public void setDevice( String deviceName , String mode ) throws Exception {
      say( "set "+deviceName+" "+mode);
   }
   public void say( String message ) throws Exception {
      long now = System.currentTimeMillis();
      Socket s = new Socket( _hostname , _port ) ;
      try{

         BufferedReader br = new BufferedReader( new InputStreamReader( s.getInputStream() ) ) ;
         PrintWriter    pw = new PrintWriter( new OutputStreamWriter( s.getOutputStream() ) ) ;
      
         pw.println( message ) ;
         pw.println("exit") ;
         pw.flush();
 
         String in = null ; 
         try{
            while( ( in = br.readLine() ) != null ){
               System.out.println(" -> "+in );
            }
         }catch( Exception eei ){
             try{ br.close() ; }catch(Exception eee ){}
             try{ pw.close() ; }catch(Exception eee ){}
         } 
      }catch(Exception ee ){
         throw ee ;
      }finally{
         try{ s.close() ; }catch(Exception eee ){}
         long diff = System.currentTimeMillis() - now ;
         System.out.println("Millis = "+diff);
      }

   } 
/*
   public void say( String message ){
       _pw.println(message);
       _pw.flush();
   }
*/
   public static void main( String [] args ) throws Exception {

      if( args.length < 2 ) 
       throw new
       IllegalArgumentException( "Usage : ... <hostName> <portNumber>");
 
      String hostname = args[0] ;
      int port = Integer.parseInt( args[1] ) ;

      TelnetDriver driver = new TelnetDriver( hostname , port ) ;

      for( int i = 2 ; i < args.length ; i++ ){
         driver.say( args[i]);
      }
   }

}
