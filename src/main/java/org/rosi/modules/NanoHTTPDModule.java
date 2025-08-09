package org.rosi.modules;
 
import org.rosi.util.RosiSetterCommand;
import org.rosi.util.RosiTimerCommand;
import org.rosi.util.RosiCommand;

import java.util.Map;
import java.util.TreeMap;
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

            /*
             *       PAGES 
             */
            if( uri.equals("/") ){
                responseString = createEmptyPage("Hallo Rosi!") ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );            

            }else if(  uri.equals( "/page/table") ){
            /* -------------------------------------------------- */
                responseString = createTablePage() ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );

            }else if( uri.equals("/request") || uri.equals("/page/submit")){
             /* -------------------------------------------------- */
                responseString = createMessageSubmitPage() ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );

            }else if( uri.equals("/page/boxes")){
             /* -------------------------------------------------- */
                responseString = createSimpleBoxPage("/api/status") ; /*boxedPage;*/
                return newFixedLengthResponse( responseCode, "text/html" , responseString ) ;

            }else if( uri.equals("/page/boxes.s")){
             /* -------------------------------------------------- */
                responseString = createSimpleBoxPage("/api/simulation") ; /*boxedPage;*/
                return newFixedLengthResponse( responseCode, "text/html" , responseString ) ;

             }else if( uri.equals("/page/dashboard")){
             /* -------------------------------------------------- */
                responseString = createComplexBoxPage("/api/status") ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString ) ;

             }else if( uri.equals("/") || uri.equals("/page/dashboard.s")){
             /* -------------------------------------------------- */
                responseString = createComplexBoxPage("/api/simulation") ;
                return newFixedLengthResponse( responseCode, "text/html" , responseString ) ;
          
            /*
             *        API
             * -----------------------------
             */
            }else if( uri.startsWith("/api/send/")){
             /* -------------------------------------------------- */
                return handleSetMessage(uri);

            }else if( uri.equals("/api/status")){
             /* -------------------------------------------------- */
                responseString = getTableJSON() ;

            }else if( uri.equals("/api/simulation")){
             /* -------------------------------------------------- */
                responseString = getSimulationJSON() ;
      
            }else{
                responseString = X404Page ;
                responseCode   = Response.Status.NOT_FOUND;
                return newFixedLengthResponse( responseCode, "text/html" , responseString );

            }

            return newFixedLengthResponse( responseCode, "application/json" , responseString );

        }
        private Response handleSetMessage( String uri ){

            String [] list = uri.split("/");
            if( list.length != 4 ){
                return newFixedLengthResponse( Response.Status.NOT_ACCEPTABLE, 
                "text/html" , 
                "Illegal Arguments!" );
            }
            String [] request = list[3].split(":") ;
            try{
                if( request.length == 3 ){
                    requestSendMessage( request[1], request[2] ) ;
                }else if( request.length == 2 ){
                    requestSendMessage( request[0] , request[1] ) ;
                }else{
                    return newFixedLengthResponse( Response.Status.NOT_ACCEPTABLE, 
                    "text/html" , 
                    "Illegal Arguments!" );
                }
            }catch(Exception eee ){
                return newFixedLengthResponse( 
                    Response.Status.NOT_ACCEPTABLE, 
                    "text/html" , 
                    "Illegal Arguments! "+eee.getMessage() );
            }
            return newFixedLengthResponse( Response.Status.OK, "text/html" , "" );
        } 
    }
    private ModuleContext        _context = null ;
    private Map<String,String[]>   map    = new HashMap<String,String[]>() ;

    public NanoHTTPDModule( String moduleName , ModuleContext context ) 
    throws Exception{

        super( moduleName , context );

        _context  = context ;

        log( "Started" ) ;

        int serverPort   = Integer.parseInt( _context.get("port"   , true )) ;

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
    /*
     *                   API Functions
     */
    private void requestSendMessage( String key , String value ) throws Exception {
        put( new RosiSetterCommand( key , value ) ) ;
    }
    private String getTableJSON(){
        Map<String, String[]> sortedMap = new TreeMap<>(map);
        StringBuffer sb = new StringBuffer() ;
        boolean start = true ;
        sb.append("[");
        for( Map.Entry<String,String[]> e : sortedMap.entrySet() ){
            if( ! start )sb.append(","); 
            start = false ;
            sb.append("{\"key\":\"").append(e.getKey()).append("\",") ;
            sb.append("\"source\":\"").append(e.getValue()[0]).append("\",") ;
            sb.append("\"value\":\"").append(e.getValue()[1]).append("\"}") ;
        }
        sb.append("]");
        return sb.toString() ;
    }
    private String getSimulationJSON(){
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        sb.append("{ \"key\" : \"livingroom.heater.battery\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"livingroom.motion.move\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.heater.battery\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.bio.temperature\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.heater.temperature\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.heater.valve\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.motion.battery\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.motion.moved\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.motion.trigger\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  },") ;
        sb.append("{ \"key\" : \"bedroom.motion.waste\" , \"value\" : \"5.5\" , \"source\" : \"engine\"  }") ;
        sb.append("]");
        return sb.toString() ;
    }
    /*
     *                   PAGES
     * -----------------------------------------------------------------------
     */
    private String createEmptyPage( String message ){
        StringBuffer sb = new StringBuffer() ;
        sb.append("<!DOCTYPE html><html lang=\"en\">") ;
        sb.append("<head>").
            append("<meta charset=\"UTF-8\">").
            append("<title>Framework Layout</title>");

        sb.append("<style>").
                append(cssComplexBody).
                append(cssComplexMenu).
            append("</style>");

        sb.append("</head>");

        sb.append("<body>").
            append(htmlComplexMenu).
            append("</body>");

        sb.append("</html>");
        return sb.toString();      
    }
    private String createComplexBoxPage( String requestString ){

        StringBuffer sb = new StringBuffer() ;
        sb.append("<!DOCTYPE html><html lang=\"en\">") ;
        sb.append("<head>").
            append("<meta charset=\"UTF-8\">").
            append("<title>Framework Layout</title>");

        sb.append("<style>").
                append(cssComplexBody).
                append(cssComplexMenu).
                append(cssComplexBox).
            append("</style>");

        sb.append("</head>");

        sb.append("<body>").
            append(htmlComplexMenu).
            append("<div id=\"statusContainer\"></div>").
            append("<script>").
            append(jsComplexMenuString).
            append(jsComplexBox(requestString)).
            append("</script>").
            append("</body>");

        sb.append("</html>");
        return sb.toString();
    }
    private String createSimpleBoxPage( String requestString ){

        StringBuffer sb = new StringBuffer() ;
        sb.append("<!DOCTYPE html><html lang=\"en\">") ;
        sb.append("<head>").
            append("<meta charset=\"UTF-8\">").
            append("<title>Framework Layout</title>");

        sb.append("<style>").
                append(cssComplexBody).
                append(cssComplexMenu).
                append(cssSimpleBoxPage).
            append("</style>");

        sb.append("</head>");

        sb.append("<body>").
            append(htmlComplexMenu).
            append("<script>").
            append(jsComplexMenuString).
            append(jsSimpleBox(requestString)).
            append("</script>").
            append("</body>");

        sb.append("</html>");
        return sb.toString();
    }
    private String createTablePage(){
        StringBuffer sb = new StringBuffer() ;
        sb.append("<!DOCTYPE html><html lang=\"en\" />");
        sb.append("<head>").
            append("<meta charset=\"UTF-8\">").
            append("<title>Sensor Value Table</title>");

        sb.append("<style>");
        sb.append(cssComplexBody);
        sb.append(cssComplexMenu);
        sb.append(cssTableBox);
        sb.append("</style>");

        sb.append("<script>");
        sb.append(jsComplexMenuString);
        sb.append("</script>");

        sb.append("</head>");
        
        sb.append("<body>");
        sb.append(htmlComplexMenu);
        sb.append(htmlTableBox);
        sb.append("</body></html>");
        
        return sb.toString();
    }
    private String createMessageSubmitPage(){
        StringBuffer sb = new StringBuffer() ;
        sb.append("<!DOCTYPE html><html lang=\"en\" />");
        sb.append("<title>Message Submit Form</title>");
        sb.append("<head><style>");
        sb.append(cssComplexBody);
        sb.append(cssComplexMenu);
        sb.append(cssMessageSubmitBox);
        sb.append("</style></head><body>");
        sb.append(htmlComplexMenu);
        sb.append(htmlMessageSubmitBox);
        sb.append("</body></html>");

        return sb.toString();
}
 
private String cssMessageSubmitBox =
    """
    .form-container {
      <!-- display: inline-block; -->
      text-align: left;
      padding: 30px;
      margin: 50px auto;
      background-color: white;
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
      text-align: center;
    }

    td {
      padding: 10px;
      color: #1e3a5f;
      text-align: left ;
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
    btn = registerButton("ClickClack",refreshTable);
    console.log("Register Button returned : "+btn);
    refreshTable();
    function refreshTable() {
      console.log("Refreshing table");
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
    }
     </script>
    """;
    private String X404Page =
    """
    <!DOCTYPE html>
    <html lang="en">
    <head>
    <meta charset="UTF-8">
    <title>404 Not Found</title>
    <style>
        html, body {
        margin: 0;
        padding: 0;
        height: 100%;
        background-color: #8B0000; /* dark red */
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-family: Arial, sans-serif;
        font-size: 10vw;
        }
    </style>
    </head>
    <body>
    404
    </body>
    </html>
""";
private String cssSimpleBoxPage =
"""
    .domain-frame {
      background-color: #f4f4f4;
      margin: 0;
      padding: 20px;
      display: flex;
      flex-wrap: wrap;
      gap: 20px;
      justify-content: center;    
    }
    .domain-box {
      background-color: #ffffff;
      box-shadow: 0 4px 8px rgba(0,0,0,0.1);
      border-radius: 8px;
      padding: 20px;
      flex: 1 1 400px;
      max-width: 600px;
      box-sizing: border-box;
    }


    .domain-title {
      font-size: 1.5em;
      font-weight: bold;
      color: #1e3a5f;
      margin-bottom: 15px;
      text-align: center;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th {
      background-color: #1e3a5f;
      color: white;
      padding: 10px;
      text-align: left;
    }

    td {
      padding: 8px;
      border-bottom: 1px solid #ddd;
      color: #333;
    }
     tr:nth-child(even) {
      background-color: #f2f2f2;
    }       
""";
private String jsSimpleBox(String requestString){

return
"""


  const btn       = registerButton( "Refresh",loadData);
  const frame     = document.createElement('div');
  frame.className = "domain-frame";

  loadData() ;

  async function loadData() {
"""
+
    "const response = await fetch('"+requestString+"');" 
+
"""
    const data = await response.json();

    const domainMap = {};

    data.forEach(item => {
      const [domain, master] = splitKey(item.key);
      if (!domainMap[domain]) {
        domainMap[domain] = [];
      }
      domainMap[domain].push({ master, value: item.value, source: item.source });
    });
    
    frame.innerHTML = "" ;

    for (const domain in domainMap) {
      const box = document.createElement('div');
      box.className = 'domain-box';

      const title = document.createElement('div');
      title.className = 'domain-title';
      title.textContent = domain;
      box.appendChild(title);

      const table = document.createElement('table');
      const thead = document.createElement('thead');
      thead.innerHTML = '<tr><th>Source</th><th>Attribute</th><th>Value</th></tr>';
      table.appendChild(thead);

      const tbody = document.createElement('tbody');
      domainMap[domain].forEach(entry => {
        const row = document.createElement('tr');
        row.innerHTML = `<td>${entry.source}</td><td>${entry.master}</td><td>${entry.value}</td>`;
        tbody.appendChild(row);
      });

      table.appendChild(tbody);
      box.appendChild(table);
      frame.appendChild(box);
    }
    document.body.appendChild(frame);
  }
 function splitKey(fullKey) {
    const parts = fullKey.split('.');
    const master = parts.pop();
    const domain = parts.join('.') || '(root)';
    return [domain, master];
  }
""";
}
/* 
  ____                      _             ____            
 / ___|___  _ __ ___  _ __ | | _____  __ | __ )  _____  __
| |   / _ \| '_ ` _ \| '_ \| |/ _ \ \/ / |  _ \ / _ \ \/ /
| |__| (_) | | | | | | |_) | |  __/>  <  | |_) | (_) >  < 
 \____\___/|_| |_| |_| .__/|_|\___/_/\_\ |____/ \___/_/\_\
                     |_|                                  
*/

private String jsComplexBox( String requestString ) {

return """

<!-- document.getElementById("updateButton").addEventListener("click", loadData ) ; -->
btn = registerButton( "Update Box", loadData ) ;

async function loadData() {
"""
+ " const response = await fetch(\""+requestString+"\");"
+ """
    console.log(response);
    const data = await response.json();

    const frameMap = {} ;

    data.forEach(item => {

        const inlist = item.key.split('.');

        const frameName  = inlist.shift() ;
        const domainName = inlist.shift() ;
        const entryName  = inlist.join('.') || '(root)' ;
    
        if( ! frameMap[frameName] )frameMap[frameName] = {} ;

        const domainMap = frameMap[frameName] ;

        if( ! domainMap[domainName] )domainMap[domainName] = {} ;

        const entryMap = domainMap[domainName] ;

        entryMap[entryName] = { value : item.value , source : item.source } ;

    }); 
    
    const statusContainer = document.getElementById("statusContainer")  ;
    statusContainer.innerHTML = "" ;
    for( const fName in  frameMap ){
        
        console.log("- "+fName);

        const frameDiv  = document.createElement('div');
        frameDiv.className = 'frame' ;
        
            const frameHead = document.createElement('h2');
            frameHead.innerHTML = fName ;
            frameDiv.appendChild(frameHead);
        
            const domainGridDiv = document.createElement('div');
            domainGridDiv.className = 'domain-grid' ;

            domainM = frameMap[fName] ;

            for( domainN in domainM ){
            
                console.log("    "+domainN) ;

                const domainDiv = document.createElement('div');
                domainDiv.className = 'domain' ;

                    const domainHead = document.createElement('h3');
                    domainHead.innerHTML = domainN ;
                    domainDiv.appendChild( domainHead ) ;

                    const entryMap = domainM[domainN] ;

                    const table = document.createElement('table');

                    const trh = document.createElement('thead');
                    trh.innerHTML = "<tr><th>Source</th><th>Key</th><th>Value</th></tr>" ;
                    table.appendChild(trh);

                    const tbody = document.createElement('tbody') ;

                    for( const entryName in entryMap ){
                        const tr = document.createElement('tr');

                        const entry = entryMap[entryName] ;

                        tr.innerHTML = `<td>${entry.source}</td><td>${entryName}</td><td>${entry.value}</td>` ; 

                        console.log("      "+entryName+";value="+entry.value+";source="+entry.source) ;

                        tbody.appendChild(tr);
                    }
                    table.appendChild(tbody);
                    domainDiv.appendChild( table );

                domainGridDiv.appendChild( domainDiv ) ;
            }
            frameDiv.appendChild(domainGridDiv) ;

        statusContainer.appendChild( frameDiv ) ;
    }
    
}
loadData(); 
""";
}
private String jsComplexMenuString = """
    function registerButton( title , clickFunction ) {
        const buttonRow = document.getElementById(\"action-menu\") ;
        const btn = document.createElement(\"button\");
        btn.innerHTML = title ;
        btn.onclick = clickFunction;
        buttonRow.appendChild(btn);
        return btn ;
    }
"""
;

private String htmlComplexMenu =
"""
    <!-- Top button bars -->
    <div class="button-bar upper">
        <button  onclick="location.href='/page/submit'">Request</button>
        <button  onclick="location.href='/page/table'">Table</button>
        <button  onclick="location.href='/page/dashboard'">Dashboard</button>
        <button  onclick="location.href='/page/dashboard.s'">Dashboard.S</button>
        <button  onclick="location.href='/page/boxes'">Boxes</button>
        <button  onclick="location.href='/page/boxes.s'">Boxes.S</button>
    </div>

    <div id="action-menu" class="button-bar lower">
    <!--
        <button id="menuUpdateButton">Update</button>
        <button disabled>Stop Service</button>
        <button disabled>Restart</button>
        <button disabled>Logs</button>
    -->
    </div>
""";
private String cssComplexMenu =
"""
   /* Top button bars */
    .button-bar {
      display: flex;
      justify-content: center;
      flex-wrap: wrap;
      background-color: #1e3a5f;
      padding: 10px;
      gap: 10px;
    }

    .button-bar button {
      background-color: #3b5998;
      color: white;
      padding: 10px 20px;
      font-size: 16px;
      border: none;
      border-radius: 5px;
      flex: 1 1 auto;
      max-width: fit-content;
    }

    .button-bar.upper button {
      min-width: 140px; /* Ensure same size in top bar */
    }

    .button-bar.lower button {
      min-width: 160px; /* Ensure same size in lower bar */
    }
      
""";
private String cssComplexBody =
"""
body {
    margin: 0;
    font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
    background-color: #f0f2f5;
}       
""";
private String cssComplexBox = 
"""
    /* Frame containers */
    .frame {
      background-color: white;
      margin: 30px 40px;
      padding: 20px;
      border-radius: 10px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .frame h2 {
      text-align: left;
      margin-top: 0;
      color: #1e3a5f;
    }
       /* Domain containers inside frames */
    .domain-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 20px;
      justify-content: space-between;
    }

    .domain {
      background-color: #f9f9f9;
      border-radius: 8px;
      padding: 15px;
      flex: 1 1 300px;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
      display: flex;
      flex-direction: column;
    }

    .domain h3 {
      margin-top: 0;
      color: #1e3a5f;
      text-align: center;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 10px;
      font-size: 14px;
    }

    th {
      background-color: #1e3a5f;
      color: white;
      padding: 8px;
      text-align: left;
    }

    td {
      padding: 8px;
      border: 1px solid #ddd;
    }

    tr:nth-child(even) {
      background-color: #f2f2f2;
    }

    tr:nth-child(odd) {
      background-color: #ffffff;
    }
    @media screen and (max-width: 600px) {
      .domain {
        flex: 1 1 100%;
      }

      .button-bar button {
        flex: 1 1 100%;
      }

      .frame {
        margin: 20px;
      }
    }    
""";

}