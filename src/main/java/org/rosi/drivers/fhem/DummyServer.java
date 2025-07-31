package org.rosi.drivers.fhem ;

import java.io.* ;
import java.net.* ;

public class DummyServer {

   public DummyServer( int port ) throws Exception {

         ServerSocket    server = new ServerSocket(port) ;

         while( true ){
            Socket _socket = server.accept() ;
            errorLog("New socket created : "+_socket);
            PrintWriter     _out = new PrintWriter( new OutputStreamWriter( _socket.getOutputStream() ) ) ;
            BufferedReader  _in  = new BufferedReader( new InputStreamReader( _socket.getInputStream() ) );

            String command = null ;
            while( ( command = _in.readLine() ) != null ){
                errorLog(command); 
                _out.println("Hallo>");
                _out.flush();
            }
            _socket.close();
            errorLog("Socket closed");
         }

   }
   private void errorLog( String message ){
      System.err.println(message);
   }
   public static void main( String [] args ) throws Exception {

      if( args.length < 1 ){     
          System.err.println("Usage : ... <port>");
          System.exit(4);
      }
      int port = Integer.parseInt( args[0] ) ;

      DummyServer server = new DummyServer( port ) ;

   }

}
