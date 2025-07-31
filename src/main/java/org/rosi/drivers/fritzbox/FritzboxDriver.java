package org.rosi.drivers.fritzbox ;

import java.security.* ;
import java.nio.*;
import java.net.*;
import javax.net.ssl.* ;
import java.io.*;
import java.util.* ;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class FritzboxDriver {

   private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
 
   private String _username = null ;
   private String _password = null ;
   
   private String _baseURL    = null ;
   private String _loginURI   = null ;
   private String _serviceURI = null ;
      
   private String _sid     = null ;

   public FritzboxDriver( String serverURL , String loginURI , String serviceURI ){
      this._baseURL    = serverURL ;
      this._loginURI   = loginURI ;
      this._serviceURI = serviceURI ;
   }
   /**
    **
     *  Helper Functions
     * -----------------------
    **/
   public static String asHex(byte[] buf) {

       char[] chars = new char[2 * buf.length];
       for (int i = 0; i < buf.length; ++i)
       {
           chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
           chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
       }
       return new String(chars);
   }
   public static String calculateMD5( String input )
      throws 
      UnsupportedEncodingException, NoSuchAlgorithmException {
   
       byte      []  x   = input.getBytes("UTF-16LE" ) ;
       MessageDigest md  = MessageDigest.getInstance( "MD5" ) ;
       byte [] thedigest = md.digest(x) ;
       
       return asHex( thedigest ) ;
   }
   /**
     *
     *  Fetch the XML document
     * -----------------------
    **/
    
   private InputStream getInputStream( String urlString ) throws Exception {
   
       URL url = new URI( urlString ).toURL() ;
       
       // System.out.println("URL : "+url);

       URLConnection connection = url.openConnection() ;
       
       if( ! ( connection instanceof HttpURLConnection ) )
           throw new
            Exception("Internal Error: connection not an HttpURLConnection" );
       
       HttpURLConnection http = (HttpURLConnection) connection ;
       //System.out.println("got the URL connection");
       
       if( http instanceof HttpsURLConnection ){

           System.out.println("Is an https connection");
       
           HttpsURLConnection shttp = (HttpsURLConnection) http ;
           
           shttp.setHostnameVerifier( 
               new HostnameVerifier(){
               
                    public boolean verify( String hostname , SSLSession session ){
                       System.out.println("Checking hostname : "+hostname ) ;
                       return true ;
                    }
               }
           );
       
       }
       //System.out.println("Asking for response code");
       
       int ret = http.getResponseCode() ;
       
       if( ret != 200 )
           throw new
           HttpRetryException("Server Error: server returned ("+ret+") "+http.getResponseMessage() , ret );

       Object o = connection.getContent() ;

       if( !( o instanceof InputStream ) )
           throw new
           Exception("URL content type is not an InputStream" );

       return (InputStream) o ;
   
   }
   public XmlDocument fetchXmlDocument(  String urlString ) throws Exception {

       InputStream inStream = getInputStream( urlString ) ;
       
       DocumentBuilder docbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder() ;

       Document doc = docbuilder.parse( inStream ) ;
       
       try{ inStream.close() ; }catch(IOException ioe ){}

       return new XmlDocument( doc.getFirstChild() ) ;


   } 
   public String fetchString(  String urlString ) throws Exception {

       InputStream inStream = getInputStream( urlString ) ;

       BufferedReader br = new BufferedReader( new InputStreamReader( inStream ) ) ;             
       
       String line = null ;
       StringBuffer sb = new StringBuffer() ;
       while( ( line = br.readLine() ) != null )sb.append(line) ;

       try{ br.close() ; }catch(IOException ioe ){}

       return  sb.toString() ;

   } 
   public void setCredentials( String username , String password ){
       _username = username ;
       _password = password ;
   }
   public void authenticate() throws Exception {
       
      FritzboxLogin login = fetchXmlDocument( _baseURL+_loginURI ).getLogin() ;
      
      if( login.isValid() ){
         System.err.println("Login Still valid" ) ;
         System.err.println(login.toString());
	 return ;
      }
      
      String challenge = login.getChallenge() ;
      
      if( challenge == null )
        throw new
	IllegalArgumentException("Server didn't return a challenge");
	
      String response = challenge + "-" + calculateMD5( challenge + "-" + _password  ) ;
      
      String urlString = _baseURL + _loginURI + "?username=" + _username + "&response=" + response  ;

      login = fetchXmlDocument( urlString ).getLogin() ;
      
      if( ( _sid = login.getSID() ) == null )
         throw new
	 IllegalArgumentException("Server didn't present a 'sid'");     
      
   
   }
   public String getSID(){ 

      if( _sid == null )
         throw new
	 IllegalArgumentException("Server didn't present a 'sid'");     


      return _sid ;
         
   }
   public FritzboxDeviceInfo [] getDeviceInfoList() throws Exception {
     return _getDeviceInfoList( getSID() ) ;
   }
   public FritzboxDeviceInfo [] _getDeviceInfoList( String sid ) throws Exception {
   
   
      String requestURL = _baseURL + _serviceURI + "?switchcmd=getdevicelistinfos&sid="+sid ;   
      
      XmlDocument doc = fetchXmlDocument( requestURL ) ;
      
//      System.out.println( doc.printXmlDocument() ) ;
      
      List<FritzboxDeviceInfo> list = doc.getFritzboxDeviceInfos() ;    
      
      return list.toArray(new FritzboxDeviceInfo[list.size()]) ;
      
   }
   public String getDeviceList() throws Exception {
       return _getDeviceList( getSID() ) ;
   }
   public String _getDeviceList( String sid ) throws Exception {
   
   
      String requestURL = _baseURL + _serviceURI + "?switchcmd=getswitchlist&sid="+sid ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   public String setDevice( String ain , int mode ) throws Exception {
     return _setDevice( getSID() , ain , mode ) ;
   }
   public String _setDevice( String sid , String ain , int mode ) throws Exception {
   
      String command = mode < 0 ? "getswitchstate" : ( mode == 0 ? "setswitchoff" : "setswitchon" ) ;
      
      String requestURL = _baseURL + _serviceURI + "?switchcmd="+command+"&sid="+sid+"&ain="+ain ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   public String _getDeviceName( String sid , String deviceID ) throws Exception {
   
   
      String requestURL = _baseURL + _serviceURI + "?switchcmd=getswitchname&sid="+sid+"&ain="+deviceID ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   public String _executeCommand( String command ) throws Exception {
   
   
      String requestURL = _baseURL + _serviceURI + "?"+command ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   public static void main( String [] args )throws Exception {

      if( args.length < 2 ){
        System.out.println("Usage : ... <serverURL> getsid <username> <password>");
        System.out.println("Usage : ... <serverURL> getdeviceinfolist <sid>");
        System.out.println("Usage : ... <serverURL> getdevicelist <sid>");
        System.out.println("Usage : ... <serverURL> ls <sid>");
        System.out.println("Usage : ... <serverURL> getdevicename <sid> <ain>");
        System.out.println("Usage : ... <serverURL> command <command>");
        System.exit(1);
      }

      String urlString      = args[0] ;
      String command        = args[1] ;
      
      
      String loginURI       = "/login_sid.lua" ;
      String serviceURI     = "/webservices/homeautoswitch.lua" ;

      FritzboxDriver driver = new FritzboxDriver( urlString , loginURI , serviceURI ) ;
       
      if( command.equals( "getsid" ) ){
      
          if( args.length < 4 ){
            System.out.println("Usage : ... <serverURL> getsid <username> <password>");
            System.exit(1);
          }
      
          String userString     = args[2] ;
          String passwordString = args[3] ;
      
      
          driver.setCredentials( userString , passwordString ) ;
          try{ 
             driver.authenticate() ;
          }catch(FileNotFoundException fnf ){
             System.out.println("Server Error. URL not found at server : "+fnf.getMessage() ) ;
          }catch(Exception ae ){
             System.out.println("Authentication Failed due to : "+ae ) ;
          }
      
          System.out.println("SID="+driver.getSID() ) ; 
      
      }else if( command.equals("getdeviceinfolist") ){
      
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> getdeviceinfolist <sid>");
            System.exit(1);
          }
          String sid     = args[2] ;
	  
	  FritzboxDeviceInfo [] infos = driver._getDeviceInfoList( sid ) ;
	  
	  for( FritzboxDeviceInfo info : infos ){
	     System.out.println( info.toString() ) ;
	  }
     
      }else if( command.equals("getdevicelist") ){
      
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> getdevicelist <sid>");
            System.exit(1);
          }
          String sid = args[2] ;
	  
	  System.out.println( driver._getDeviceList( sid )  );
     
      }else if( command.equals("ls") ){
      
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> ls <sid>");
            System.exit(1);
          }
          String sid = args[2] ;

	  FritzboxDeviceInfo [] infos = driver._getDeviceInfoList( sid ) ;
	  for( int i = 0 ; i < infos.length ; i++ ){
	     System.out.println( infos[i] ) ;
          } 
	  Map<String,FritzboxDeviceInfo> map = new HashMap<String,FritzboxDeviceInfo>() ;
	  
	  for( int i = 0 ; i < infos.length ; i++ ){
	  
	     map.put( infos[i].getName() ,  infos[i] ) ;
	     
	  }
	  
	  String deviceList = driver._getDeviceList( sid ) ;
	  
	  String [] deviceIDs = deviceList.split(",") ;
	  
	  String [] deviceName = new String[deviceIDs.length] ;
	  String [] deviceMode = new String[deviceIDs.length] ;
	  
	  for( int i = 0 ; i < deviceName.length ; i++ ){
	  
	      deviceName[i] = driver._getDeviceName( sid , deviceIDs[i] ) ;
	      deviceMode[i] = driver._setDevice( sid , deviceIDs[i] , -1 ) ;
	      
	      FritzboxDeviceInfo info = map.get( deviceName[i]  ) ;
	      
	      if( info != null ){
	      
	           info.setDeviceID( deviceIDs[i] ) ;
		   info.setState( deviceMode[i] ) ;
	      }
	  }
	  for( int i = 0 ; i < infos.length ; i++ ){
	  
	     System.out.println( infos[i] ) ;
	     
	  }
     
      }else if( command.equals("device") ){
      
          if( args.length < 5 ){
            System.out.println("Usage : ... <serverURL> device <sid> <ain> on|off|query");
            System.exit(1);
          }
          String sid  = args[2] ;
	  String aid  = args[3] ;
	  String mode = args[4] ;
	  
	  if( mode.equals("on") ){
	  
	      System.out.println( driver._setDevice( sid , aid , 1 ) ) ;
	      
	  }else if( mode.equals("off") ){

	      System.out.println( driver._setDevice( sid , aid , 0 ) ) ;
	  
	  }else if( mode.equals("query") ) {

	      System.out.println( driver._setDevice( sid , aid , -1 ) ) ;
	  
	  }else{
	      System.out.println(  "Unkown command" );
	  } 
     
      }else if( command.equals("getdevicename") ){
      
          if( args.length < 4 ){
            System.out.println("Usage : ... <serverURL> getdevicename <sid> <deviceID>");
            System.exit(1);
          }
          String sid      = args[2] ;
	  String deviceID = args[3] ;
	  
	  System.out.println( driver._getDeviceName( sid , deviceID )  );
     
      }else if( command.equals("command") ){
      
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> command <command>");
            System.exit(1);
          }
          String com     = args[2] ;
	  
	  System.out.println( driver._executeCommand( com )  );
     
      }
   }
}

