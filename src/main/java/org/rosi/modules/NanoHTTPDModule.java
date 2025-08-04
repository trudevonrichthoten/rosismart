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

        private RosiWebServer( int port ) throws IOException {
            super(port);
            log("Server started at port : " + port ) ;
            start( NanoHTTPD.SOCKET_READ_TIMEOUT , false ) ;
        }
        @Override
        public Response serve( IHTTPSession session ){

            String responseString = "" ;
            Response.Status responseCode = Response.Status.OK ;

            String uri = session.getUri() ;

            if( uri.equals("/status")){
                responseString = getTableHTML() ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );        
            }else if( uri.equals("/request")){
                responseString = getSubmitPage() ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );        
            }else if( uri.startsWith("/api/send/")){
                String [] list = uri.split("/");
                if( list.length != 4 ){
                    return newFixedLengthResponse( Response.Status.NOT_ACCEPTABLE, "text/html" , "Illegal Arguments!" );
                }
                String [] request = list[3].split(":") ;
                try{
                    if( request.length == 3 ){
                        requestSendMessage( request[1], request[2] ) ;
                    }else if( request.length == 2 ){
                        requestSendMessage( request[0] , request[1] ) ;
                    }else{
                        return newFixedLengthResponse( Response.Status.NOT_ACCEPTABLE, "text/html" , "Illegal Arguments!" );
                    }
                }catch(Exception eee ){
                    return newFixedLengthResponse( 
                        Response.Status.NOT_ACCEPTABLE, 
                        "text/html" , 
                        "Illegal Arguments! "+eee.getMessage() );
                }
            }else if( uri.equals("/api/status")){
                responseString = getTableJSON() ;
            }else if( uri.equals("/") ){
                responseString = getApiTablePage() ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );
            }else{
                responseString = "404 Knock Knock" ;
                responseCode   = Response.Status.NOT_FOUND;
            }

            return newFixedLengthResponse( responseCode, "application/json" , responseString );

        }

    }
    private void requestSendMessage( String key , String value ) throws Exception {
        put( new RosiSetterCommand( key , value ) ) ;
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
    public String getTableJSON(){
        StringBuffer sb = new StringBuffer() ;
        boolean start = true ;
        sb.append("[");
        for( Map.Entry<String,String> e : map.entrySet() ){
            if( ! start )sb.append(","); 
            start = false ;
            sb.append("{\"key\":\"").append(e.getKey()).append("\",") ;
            sb.append("\"value\":\"").append(e.getValue()).append("\"}") ;
        }
        sb.append("]");
        return sb.toString() ;
    }
    public String getTableHTML(){
        String responseHeader = "<html><body><h1>Rosi's Value</h1>" ;
        String responseFooter = 
            "<address>This is Rosi's data page!</address></body></html>" ;

        String css = getTableCSS();

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
        return sb.toString() ;  
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
    public String getApiTablePage(){
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>JSON Table Viewer</title>
  <style>
    body {
      font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
      background-color: #f5f5f5;
      margin: 0;
      padding: 2em;
      display: flex;
      justify-content: center;
    }

    .container {
      background-color: #ffffff;
      padding: 2em;
      border-radius: 12px;
      box-shadow: 0 0 12px rgba(0, 0, 0, 0.2);
      width: 100%;
      max-width: 800px;
      text-align: center;
    }

    button {
      padding: 0.7em 1.5em;
      font-size: 1em;
      background-color: #004080;
      color: white;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      margin-bottom: 1.5em;
    }
      table {
      width: 100%;
      border-collapse: collapse;
    }

    thead {
      background-color: #003366;
      color: white;
    }

    th, td {
      padding: 0.8em;
      text-align: left;
    }

    tbody tr:nth-child(even) {
      background-color: #f0f0f0;
    }

    tbody tr:nth-child(odd) {
      background-color: #ffffff;
    }

    td {
      color: #003366;
    }

    th:nth-child(1), td:nth-child(1) {
      text-align: left;
    }

    th:nth-child(2), td:nth-child(2) {
      text-align: left;
    }

    th:nth-child(3), td:nth-child(3) {
      text-align: right;
    }
  </style>
 </head>
<body>
  <div class="container">
    <button onclick="updateTable()">Update</button>
    <table id="data-table">
      <thead>
        <tr>
          <th>Source</th>
          <th>Key</th>
          <th>Value</th>
        </tr>
      </thead>
      <tbody>
        <!-- Dynamic content goes here -->
      </tbody>
    </table>
  </div>

  <script>
    async function updateTable() {
      try {
        const response = await fetch('/api/status');
        if (!response.ok) throw new Error("Server error");

        const data = await response.json();

        const tbody = document.querySelector("#data-table tbody");
        tbody.innerHTML = ''; // Clear old content

        data.forEach(item => {
          const row = document.createElement("tr");

          const sourceCell = document.createElement("td");
          sourceCell.textContent = item.source;

          const keyCell = document.createElement("td");
          keyCell.textContent = item.key;

          const valueCell = document.createElement("td");
          valueCell.textContent = item.value;

          row.appendChild(sourceCell);
          row.appendChild(keyCell);
          row.appendChild(valueCell);

          tbody.appendChild(row);
        });
      } catch (error) {
        alert("Failed to fetch data: " + error.message);
      }
    }
  </script>
</body>
</html>        
""";
    }
    public String getTableCSS(){
        return """
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
    }
    public String getSubmitPage(){
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Key-Value Submit</title>
  <style>
    body {
      font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
      background-color: #f0f0f0;
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
    }
    
    .form-container {
      background-color: #e0f2ff;
      padding: 2em;
      border-radius: 12px;
      box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
      width: 100%;
      max-width: 600px;
    }
  
    label {
      display: block;
      margin-top: 1em;
      font-weight: bold;
      color: #003366;
    }

    input[type="text"] {
      width: 100%;
      padding: 0.5em;
      font-size: 1em;
      box-sizing: border-box;
      border: 1px solid #ccc;
      border-radius: 6px;
      background-color: white;
    }
    
      button {
      margin-top: 1.5em;
      padding: 0.6em 1.2em;
      font-size: 1em;
      background: #004080;
      color: white;
      border: none;
      border-radius: 6px;
      cursor: pointer;
    }

    button:disabled {
      background: #bbb;
      cursor: not-allowed;
    }
  </style>
</head>
<body>
  <div class="form-container">
    <label for="target">Target</label>
    <input type="text" id="target" name="target" placeholder="Enter target" minlength="1" />

    <label for="key">Key</label>
    <input type="text" id="key" name="key" placeholder="Enter key" minlength="1" />

    <label for="value">Value</label>
    <input type="text" id="value" name="value" placeholder="Enter value" minlength="1" />

    <button id="submitBtn" disabled>Submit</button>
  </div>

  <script>
    const targetInput = document.getElementById('target');
    const keyInput = document.getElementById('key');
    const valueInput = document.getElementById('value');
    const submitBtn = document.getElementById('submitBtn');

    function updateButtonState() {
      const allFilled = targetInput.value.trim() && keyInput.value.trim() && valueInput.value.trim();
      submitBtn.disabled = !allFilled;
    }

    [targetInput, keyInput, valueInput].forEach(input =>
      input.addEventListener('input', updateButtonState)
    );

    submitBtn.addEventListener('click', () => {
      const target = encodeURIComponent(targetInput.value.trim());
      const key = encodeURIComponent(keyInput.value.trim());
      const value = encodeURIComponent(valueInput.value.trim());
      const combined = `${target}:${key}:${value}`;
      const url = `/api/send/${combined}`;
      fetch(url)
        .then(response => {
          if (response.ok) {
            alert("Submitted successfully!");
          } else {
            alert("Failed to submit.");
          }
        })
        .catch(err => {
          alert("Error: " + err);
        });
    });
  </script>
</body>
</html>
""";
    }
}
