package org.rosi.modules;

import org.rosi.modules.ModuleContext;
import org.rosi.modules.RosiModule ;
import org.rosi.modules.engine.BasicEnginable;
import org.rosi.modules.RosiCommandProcessor ;

import org.rosi.util.RosiRuntimeException;
import org.rosi.util.RosiSetterCommand;
import org.rosi.util.RosiTimerCommand;
import org.rosi.util.RosiLogable ;
import org.rosi.util.RosiCommand;
import org.rosi.util.RosiLogAdapter ;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap ;
import java.io.IOException ;

import fi.iki.elonen.NanoHTTPD ;

public class NanoHTTPDModule extends RosiModule {

    private class RosiWebServer extends NanoHTTPD {
        private String responseHeader = 
            "<html><body><h1>Rosi's Value</h1>" ;
        private String responseFooter = 
            "<address>This is Rosi's data page!</address></body></html>" ;
        private String css = """
  <meta charset="UTF-8">
  <style>
    body {
      font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
      margin: 2em;
    }

    table {
      border-collapse: collapse;
      width: 100%;
      max-width: 600px;
      box-shadow: 0 0 10px rgba(0,0,0,0.1);
    }

    thead {
      background-color: #003366; /* dark blue */
      color: white;
    }

    th, td {
      padding: 12px 16px;
    }

    tbody tr:nth-child(even) {
      background-color: #f2f2f2; /* light gray */
    }

    tbody tr:nth-child(odd) {
      background-color: #ffffff; /* white */
    }

    tbody td {
      color: #003366; /* dark blue text */
    }

    td:first-child {
      text-align: left;
    }

    td:last-child {
      text-align: right;
    }
  </style>
                """;
        private RosiWebServer( int port ) throws IOException {
            super(port);
            log("Server started at port : " + port ) ;
            start( NanoHTTPD.SOCKET_READ_TIMEOUT , false ) ;
        }
        @Override
        public Response serve( IHTTPSession session ){

            String uri = session.getUri() ;

            StringBuffer sb = new StringBuffer() ;

            sb.append("<html>").
                append("<head>").
                append(css).
                append("</head><body>").
                append("<table border=1>").
                append("<thead><tr><th>Key</th><th>Value</th></tr></thead><tbody>");
            for( Map.Entry<String,String> e : map.entrySet() ){
                sb.append("<tr><td>").
                    append(e.getKey()).
                    append("</td><td>").
                    append(e.getValue()).
                    append("</td></tr>") ;
            }
            sb.append("</tbody></table>").append(responseFooter);

            return newFixedLengthResponse( Response.Status.OK , "text/html" , sb.toString() );

        }

    }
    private ModuleContext      _context = null ;
    private Map<String,String> map      = new HashMap<String,String>() ;

    public NanoHTTPDModule( String moduleName , ModuleContext context ) 
    throws Exception{

        super( moduleName , context );

        _context  = context ;

        log( "Started" ) ;

        int serverPort   = Integer.parseInt( context.get("port"   , true )) ;

        new RosiWebServer( serverPort ) ;

    }

    public void run(){

        log("Starting with debug : "+isDebugMode());

        log("Starting now.") ;

        try{

            while(true){

                try{

                    RosiCommand command  = take() ;

                    if( command instanceof RosiTimerCommand ){

                        debug("Timer  '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;                            


                    }else if( command instanceof RosiSetterCommand ){

                        debug("Setter '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

                        RosiSetterCommand setter = (RosiSetterCommand)command ;

                        this.map.put( setter.getKey() , setter.getValue() ) ;

                    }else{

                        errorLog("Unkown '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;
                
                    } 
                }catch(Exception ee ){

                }
            }
        }catch(Exception ee ){

        }
    }
    public void setCommandProcessor( RosiCommandProcessor processor ){

    }
}
