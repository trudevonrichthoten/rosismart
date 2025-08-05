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
            if( uri.equals("/") || uri.equals( "/page/table") ){
            /* -------------------------------------------------- */
                responseString = createTableBoxPage() ; /* getApiTablePageMenu() ; */
                return newFixedLengthResponse( responseCode, "text/html" , responseString );

            }else if( uri.equals("/xxx") ){
            /* -------------------------------------------------- */
                responseString = getApiTablePageMenu() ; 
                return newFixedLengthResponse( responseCode, "text/html" , responseString );

            }else if( uri.equals("/status")){
            /* -------------------------------------------------- */
                responseString = getTableHTML() ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );

            }else if( uri.equals("/request") || uri.equals("/page/submit")){
             /* -------------------------------------------------- */
                responseString = createMessageSubmitPage() ; /* getSubmitPage() ; */
                return newFixedLengthResponse( responseCode, "text/html" , responseString );

            }else if( uri.startsWith("/api/send/")){
             /* -------------------------------------------------- */
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
             /* -------------------------------------------------- */
                responseString = getTableJSON() ;
      
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
    private Map<String,String[]> map      = new HashMap<String,String[]>() ;

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
        for( Map.Entry<String,String[]> e : map.entrySet() ){
            if( ! start )sb.append(","); 
            start = false ;
            sb.append("{\"key\":\"").append(e.getKey()).append("\",") ;
            sb.append("\"source\":\"").append(e.getValue()[0]).append("\",") ;
            sb.append("\"value\":\"").append(e.getValue()[1]).append("\"}") ;
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
        for( Map.Entry<String,String[]> e : map.entrySet() ){
            sb.append("<tr><td>").
                append(e.getKey()).
                append("</td><td>").
                append(e.getValue()[1]).
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
                        String [] ar = new String[2] ;
                        ar[0] = setter.getSource() ;
                        ar[1] = setter.getValue () ;

                        this.map.put( 
                                    setter.getKey() , 
                                    new String[] { setter.getSource() , setter.getValue() }  
                                    ) ;

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
    public String getApiTablePageMenu(){
        return """
   <!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>API Data Viewer</title>
  <style>
    body {
      font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
      background-color: #f0f4f8;
      margin: 0;
      padding: 0;
      text-align: center;
    }

    /* Menu styling */
    .menu {
      background-color: #1e3a5f;
      padding: 10px 0;
      box-shadow: 0 2px 4px rgba(0,0,0,0.2);
    }

    .menu button {
      background-color: #3b5998;
      border: none;
      color: white;
      padding: 10px 20px;
      margin: 0 10px;
      font-size: 16px;
      cursor: pointer;
      border-radius: 5px;
      transition: background-color 0.3s;
    }

    .menu button:hover {
      background-color: #2d4373;
    }

    .container {
      max-width: 800px;
      margin: 30px auto;
      padding: 20px;
      background-color: #ffffff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      border-radius: 10px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 20px;
    }
    th {
      background-color: #1e3a5f;
      color: white;
      padding: 10px;
      text-align: left;
    }

    td {
      padding: 10px;
      color: #1e3a5f;
    }

    tr:nth-child(even) {
      background-color: #f9f9f9;
    }

    tr:nth-child(odd) {
      background-color: #ffffff;
    }

    #updateButton {
      margin-bottom: 20px;
      padding: 10px 20px;
      font-size: 16px;
      background-color: #1e3a5f;
      color: white;
      border: none;
      border-radius: 5px;
      cursor: pointer;
    }

    #updateButton:hover {
      background-color: #16314a;
    }
  </style>
</head>
<body>
  <!-- Menu bar -->
  <div class="menu">
    <button onclick="location.href='/'">Variable Table</button>
    <button onclick="location.href='/request'">Variable Submission</button>
    <button onclick="location.href='page3.html'">Page 3</button>
    <button onclick="location.href='page4.html'">Page 4</button>
  </div>

  <!-- Main container -->
  <div class="container">
    <button id="updateButton">Update</button>
    <table id="dataTable">
      <thead>
        <tr>
          <th>Source</th>
          <th>Key</th>
          <th>Value</th>
        </tr>
      </thead>
      <tbody>
        <!-- Data rows will be inserted here -->
      </tbody>
    </table>
  </div>

  <script>
    document.getElementById("updateButton").addEventListener("click", function() {
      fetch("/api/status")
        .then(response => response.json())
        .then(data => {
          const tableBody = document.getElementById("dataTable").querySelector("tbody");
          tableBody.innerHTML = ""; // Clear old rows

          data.forEach(item => {
            const row = document.createElement("tr");

            const sourceCell = document.createElement("td");
            sourceCell.textContent = item.source;
            row.appendChild(sourceCell);

            const keyCell = document.createElement("td");
            keyCell.textContent = item.key;
            row.appendChild(keyCell);

            const valueCell = document.createElement("td");
            valueCell.textContent = item.value;
            row.appendChild(valueCell);

            tableBody.appendChild(row);
          });
        })
        .catch(error => {
          console.error("Error fetching data:", error);
        });
    });
  </script>

</body>
</html>    
""";


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
    /*
     * -----------------------------------------------------------------------
     */
    private String createTableBoxPage(){
        StringBuffer sb = new StringBuffer() ;
        sb.append("<!DOCTYPE html><html lang=\"en\" />");
        sb.append("<title>Message Table</title>");
        sb.append("<head><style>");
        sb.append(cssBody);
        sb.append(cssMenu);
        sb.append(cssTableBox);
        sb.append("</style></head><body>");
        sb.append(htmlMenu);
        sb.append(htmlTableBox);
        sb.append("</body></html>");
        
        return sb.toString();
    }
    private String createMessageSubmitPage(){
        StringBuffer sb = new StringBuffer() ;
        sb.append("<!DOCTYPE html><html lang=\"en\" />");
        sb.append("<title>Message Submit Form</title>");
        sb.append("<head><style>");
        sb.append(cssBody);
        sb.append(cssMenu);
        sb.append(cssMessageSubmitBox);
        sb.append("</style></head><body>");
        sb.append(htmlMenu);
        sb.append(htmlMessageSubmitBox);
        sb.append("</body></html>");

        return sb.toString();
    }
    private String cssBody = 
    """        
    body {
      font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
      background-color: #f0f4f8;
      margin: 0;
      padding: 0;
      text-align: center;
    }
    """;

    private String cssMenu =
    """
    /* Menu Bar (shared) */
    .menu {
      background-color: #1e3a5f;
      padding: 10px 0;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .menu button {
      background-color: #3b5998;
      border: none;
      color: white;
      padding: 10px 20px;
      margin: 0 10px;
      font-size: 16px;
      cursor: pointer;
      border-radius: 5px;
      transition: background-color 0.3s;
    }

    .menu button:hover {
      background-color: #2d4373;
    }
    """;

    private String cssMessageSubmitBox =
    """
    .form-container {
      display: inline-block;
      text-align: left;
      padding: 30px;
      margin: 50px auto;
      background-color: #e6f0ff;
      border-radius: 10px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      width: 500px;
    }

    .form-container label {
      font-weight: bold;
      display: block;
      margin-bottom: 5px;
      color: #1e3a5f;
    }

    .form-container input {
      width: 100%;
      padding: 10px;
      margin-bottom: 20px;
      border: 1px solid #ccc;
      border-radius: 5px;
      font-size: 16px;
    }

    #submitBtn {
      width: 100%;
      padding: 10px;
      font-size: 16px;
      background-color: #1e3a5f;
      color: white;
      border: none;
      border-radius: 5px;
      cursor: pointer;
    }

    #submitBtn:disabled {
      background-color: #aaa;
      cursor: not-allowed;
    }   
    """;
    private String cssTableBoxx = 
    """
    .table-box {
      display: inline-block;
      margin: 50px auto;
      background-color: white;
      border-radius: 10px;
      padding: 20px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    table {
      border-collapse: collapse;
      width: 100%;
    }

    th {
      background-color: #1e3a5f;
      color: white;
      padding: 10px; 
      text-align: left;
    }

    td {
      padding: 10px;
      color: #1e3a5f;
    }

    tr:nth-child(even) {
      background-color: #f9f9f9;
    }

    tr:nth-child(odd) {
      background-color: #ffffff;
    }

    #updateBtn {
      margin-top: 20px;
      padding: 10px 20px;
      font-size: 16px;
      background-color: #1e3a5f;
      color: white;
      border: none;
      border-radius: 5px;
      cursor: pointer;
    }
    """;

    private String htmlMenu = 
    """
    <!-- Menu (shared) -->
    <div class="menu">
        <button onclick="location.href='/page/submit'">Message Submit</button>
        <button onclick="location.href='/page/table'">Variable Table</button>
        <button onclick="location.href='/'">Trude</button>
        <button onclick="location.href='/'">Rosi</button>
    </div>
    """;

    private String htmlMessageSubmitBox =
    """
    <!-- Form Box -->
    <div class="form-container">
        <label for="target">Target</label>
        <input type="text" id="target" placeholder="Automatic" value="Automatic" disabled />

        <label for="key">Key</label>
        <input type="text" id="key" placeholder="Key" />

        <label for="value">Value</label>
        <input type="text" id="value" placeholder="Value" />

        <button id="submitBtn" disabled>Submit</button>
    </div>

    <script>
        const targetInput = document.getElementById('target');
        const keyInput = document.getElementById('key');
        const valueInput = document.getElementById('value');
        const submitBtn = document.getElementById('submitBtn');

        function checkInputs() {
            submitBtn.disabled = !(
                targetInput.value.trim() &&
                keyInput.value.trim() &&
                valueInput.value.trim()
            );
        }

        [targetInput, keyInput, valueInput].forEach(input =>
        input.addEventListener('input', checkInputs)
        );

        submitBtn.addEventListener('click', () => {
        const request = `${targetInput.value.trim()}:${keyInput.value.trim()}:${valueInput.value.trim()}`;
        fetch(`/api/send/${encodeURIComponent(request)}`)
            .then(response => {
            if (response.ok) {
                alert('Submitted successfully!');
            } else {
                alert('Submission failed.');
            }
            })
            .catch(error => {
            console.error('Error:', error);
            alert('Error during submission.');
            });
        });
    </script>
    """;
    private String htmlTableBoxx = 
    """  
     <div class="table-box">
        <button id="updateBtn">Update</button>
        <table id="dataTable">
        <thead>
            <tr>
            <th>Source</th>
            <th>Key</th>
            <th>Value</th>
            </tr>
        </thead>
        <tbody></tbody>
        </table>
    </div>
  <div class="container">
    <button id="updateButton">Update</button>
    <table id="dataTable">
      <thead>
        <tr>
          <th>Source</th>
          <th>Key</th>
          <th>Value</th>
        </tr>
      </thead>
      <tbody>
        <!-- Data rows will be inserted here -->
      </tbody>
    </table>
  </div>
    <script>
    document.getElementById("updateBtn").addEventListener("click", () => {
      fetch("/api/status")
        .then(res => res.json())
        .then(data => {
          const tbody = document.querySelector("#dataTable tbody");
          tbody.innerHTML = "";
          data.forEach(item => {
            const row = document.createElement("tr");
            row.innerHTML = `
              <td>${item.source}</td>
              <td>${item.key}</td>
              <td>${item.value}</td>
            `;
            tbody.appendChild(row);
          });
        })
        .catch(err => {
          console.error("Error fetching data:", err);
        });
    });
    </script>
    """;

    private String cssTableBox = 
    """
    .container {
      max-width: 800px;
      margin: 30px auto;
      padding: 20px;
      background-color: #ffffff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      border-radius: 10px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 20px;
    }
    th {
      background-color: #1e3a5f;
      color: white;
      padding: 10px;
      text-align: left;
    }

    td {
      padding: 10px;
      color: #1e3a5f;
    }

    tr:nth-child(even) {
      background-color: #f9f9f9;
    }

    tr:nth-child(odd) {
      background-color: #ffffff;
    }

    #updateButton {
      margin-bottom: 20px;
      padding: 10px 20px;
      font-size: 16px;
      background-color: #1e3a5f;
      color: white;
      border: none;
      border-radius: 5px;
      cursor: pointer;
    }

    #updateButton:hover {
      background-color: #16314a;
    }

    """;

    private String htmlTableBox = 
    """
  <!-- Main container -->
  <div class="container">
    <button id="updateButton">Update</button>
    <table id="dataTable">
      <thead>
        <tr>
          <th>Source</th>
          <th>Key</th>
          <th>Value</th>
        </tr>
      </thead>
      <tbody>
        <!-- Data rows will be inserted here -->
      </tbody>
    </table>
  </div>

    <script>
    document.getElementById("updateButton").addEventListener("click", function() {
      fetch("/api/status")
        .then(response => response.json())
        .then(data => {
          const tableBody = document.getElementById("dataTable").querySelector("tbody");
          tableBody.innerHTML = ""; // Clear old rows

          data.forEach(item => {
            const row = document.createElement("tr");

            const sourceCell = document.createElement("td");
            sourceCell.textContent = item.source;
            row.appendChild(sourceCell);

            const keyCell = document.createElement("td");
            keyCell.textContent = item.key;
            row.appendChild(keyCell);

            const valueCell = document.createElement("td");
            valueCell.textContent = item.value;
            row.appendChild(valueCell);

            tableBody.appendChild(row);
          });
        })
        .catch(error => {
          console.error("Error fetching data:", error);
        });
    });
     </script>
    """;
}