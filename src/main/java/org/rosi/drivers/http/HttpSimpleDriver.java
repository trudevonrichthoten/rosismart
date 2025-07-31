package org.rosi.drivers.http ;

import java.security.* ;
import java.nio.*;
import java.net.*;
import javax.net.ssl.* ;
import java.io.*;
import java.util.* ;

public class HttpSimpleDriver {

   private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
 
   private String _username = null ;
   private String _password = null ;
   
   private String _baseURL    = null ;
      
   private String _sid     = "X123BC" ;

   public HttpSimpleDriver( String serverURL ){
      this._baseURL    = serverURL ;
   }
   /**
     *
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
   public String fetchString(  String urlString ) throws Exception {

       InputStream inStream = getInputStream( urlString ) ;

       BufferedReader br = new BufferedReader( new InputStreamReader( inStream ) ) ;             
       
       String line = null ;
       StringBuffer sb = new StringBuffer() ;
       while( ( line = br.readLine() ) != null )sb.append(line).append("\n") ;

       try{ br.close() ; }catch(IOException ioe ){}

       return  sb.toString() ;

   } 
   public void setCredentials( String username , String password ){
       _username = username ;
       _password = password ;
   }
   public void authenticate() throws Exception {
       
      if( _sid == null )
         throw new
	 IllegalArgumentException("Server didn't present a 'sid'");     
      
   
   }
   public String getSID(){ 

      if( _sid == null )
         throw new
	 IllegalArgumentException("Server didn't present a 'sid'");     


      return _sid ;
         
   }
   /**
     *
     */
   public HttpDeviceInfo [] getDeviceInfoList() throws Exception {
   /* ---------------------------------------------------------------------- */
     return _getDeviceInfoList( getSID() ) ;
   }
   /**
     *
     */
   public HttpDeviceInfo [] _getDeviceInfoList( String sid ) throws Exception {
   /* ---------------------------------------------------------------------- */
   
      List<HttpDeviceInfo> list = new ArrayList<HttpDeviceInfo>() ;
 
          String listString = _getDeviceList( sid )  ;
          String [] deviceList = listString.split(";");
          for( int i = 0 ; i < deviceList.length ; i++ ){
             String deviceDetail = deviceList[i].trim() ;
             String [] allids = deviceDetail.split(",") ;
             String id     = allids[0].trim() ;
             if( id.equals("") )continue ;
             try{

                list.add( _getDeviceInfo( sid  , id ) ) ;

             }catch(Exception ee ){
                System.out.println("Server problem with ID : "+id ) ;
             }

          }
      
      return list.toArray(new HttpDeviceInfo[list.size()]) ;
      
   }
   /**
     *
     */
   public void setDeviceInfo( HttpDeviceInfo info ) throws Exception {
      _setDeviceInfo( getSID() , info );
   }
   public void _setDeviceInfo( String sid , HttpDeviceInfo info ) throws Exception {
   /* ---------------------------------------------------------------------- */

      StringBuffer request = new StringBuffer() ;
      request.append( _baseURL ).append( "/set/" ).append(info.getDeviceId()).append("?");
      List<HttpDeviceInfo.Attribute> list = info.getAttributes() ;
      for( HttpDeviceInfo.Attribute attr : list ){
         if( attr.isModified() ){
             request.append(attr.getName()).append("=").append(attr.getValue()).append("&");
         }
      }
      String requestURL = request.toString() ;

      fetchString( requestURL ) ;
   }
   /**
     *
     */
   public HttpDeviceInfo getDeviceInfo( String deviceId ) throws Exception {
   /* ---------------------------------------------------------------------- */
       return _getDeviceInfo( getSID()  , deviceId )  ;
   }
   /**
     *
     */
   public HttpDeviceInfo _getDeviceInfo( String sid , String deviceId ) throws Exception {
   /* ---------------------------------------------------------------------- */

      String requestURL = _baseURL + "/info/" + deviceId ;
           
      String text = fetchString( requestURL ) ;

      HttpDeviceInfo info = HttpDeviceInfo.parsePlain( text ) ;
            
      return info ;
   }
   /**
     *
     */
   public String getDeviceList() throws Exception {
   /* ---------------------------------------------------------------------- */
       return _getDeviceList( getSID() ) ;
   }
   /**
     *
     */
   public String _getDeviceList( String sid ) throws Exception {
   /* ---------------------------------------------------------------------- */
   
   
      String requestURL = _baseURL + "/listdevices" ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   /**
     *
     */
   public void setDevice( String ain , String mode ) throws Exception {
   /* ---------------------------------------------------------------------- */
       _setDevice( getSID() , ain , mode ) ;
   }
   /**
     *
     */
   public void _setDevice( String sid , String attributeName , String mode ) throws Exception {
   /* ---------------------------------------------------------------------- */

       String [] a = attributeName.split("\\.") ;
       if( a.length != 2 )
          throw new
          IllegalArgumentException("AtributeName : <deviceId>.<attributeName> "+attributeName+" "+a.length);

       HttpDeviceInfo info = _getDeviceInfo( sid , a[0] ) ;

       HttpDeviceInfo.Attribute attr = info.getAttribute(a[1]) ;

       attr.setValue( mode  );

       _setDeviceInfo( sid , info ) ;
   
       return ;
   }
   public String _getDeviceName( String sid , String deviceID ) throws Exception {
   
   
      String requestURL = _baseURL + "/get?sid="+sid+"&ain="+deviceID ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   public String _executeCommand( String command ) throws Exception {
   
   
      String requestURL = _baseURL + "?"+command ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   public static String printDeviceInfo( HttpDeviceInfo info ){


          StringBuffer sb = new StringBuffer() ; 
          sb.append("[").append(info.getDeviceId() ).append("]\n");
          sb.append("    Device Name : ").append(info.getDeviceName()).append("\n");
          sb.append("    Device Type : ").append(info.getDeviceType()).append("\n");
          List<HttpDeviceInfo.Attribute> list = info.getAttributes();
          for( HttpDeviceInfo.Attribute attr : list ){
            sb.append("           ").append(attr.getName()).append(" : ").append(attr.getValue()).append("\n");
          }
          return sb.toString() ;
   }
   public static void main( String [] args )throws Exception {

      if( args.length < 2 ){
/*
        System.out.println("Usage : ... <serverURL> getsid <username> <password>");
        System.out.println("Usage : ... <serverURL> getdeviceinfolist <sid>");
*/
        System.out.println("Usage : ... <serverURL> getdevicelist <sid>");
        System.out.println("Usage : ... <serverURL> getdeviceinfo <sid> <deviceId>");
/*
        System.out.println("Usage : ... <serverURL> ls <sid>");
        System.out.println("Usage : ... <serverURL> getdevicename <sid> <ain>");
        System.out.println("Usage : ... <serverURL> command <command>");
*/
        System.exit(1);
      }

 
      try{
         runMain( args ) ;
      }catch(Exception ee ){
         System.err.println("Exception : ("+ee.getMessage()+")");

         System.exit(4);
      }
 
      System.exit(0);
 }
 public static void runMain( String [] args ) throws Exception {

      String urlString      = args[0] ;
      String command        = args[1] ;
      HttpSimpleDriver driver = new HttpSimpleDriver( urlString ) ;
       
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
	  
	  HttpDeviceInfo [] infos = driver._getDeviceInfoList( sid ) ;
	  
	  for( HttpDeviceInfo info : infos ){
	     System.out.println( info.toString() ) ;
	  }
     
      }else if( command.equals("getdeviceinfo") ){
          if( args.length < 4 ){
            System.out.println("Usage : ... <serverURL> getdeviceinfo <sid> <deviceID>");
            System.exit(1);
          }
          String sid      = args[2] ;
          String deviceId = args[3] ;

	  HttpDeviceInfo info =  driver._getDeviceInfo( sid  , deviceId )  ;

          System.out.println(printDeviceInfo(info));

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
          String listString = driver._getDeviceList( sid )  ;
          String [] deviceList = listString.split(";");
          for( int i = 0 ; i < deviceList.length ; i++ ){
             String deviceDetail = deviceList[i].trim() ;
             String [] allids = deviceDetail.split(",") ;
             String id     = allids[0].trim() ; 
             if( id.equals("") )continue ;
                try{
	           HttpDeviceInfo info =  driver._getDeviceInfo( sid  , id )  ;
                   System.out.println(printDeviceInfo( info ));
                }catch(Exception ee ){
                   System.out.println("Server problem with ID : "+id ) ;
                }
 
          }
/*
      }else if( command.equals("ls2") ){
      
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> ls <sid>");
            System.exit(1);
          }
          String sid = args[2] ;

	  HttpDeviceInfo [] infos = driver._getDeviceInfoList( sid ) ;
	  for( int i = 0 ; i < infos.length ; i++ ){
	     System.out.println( infos[i] ) ;
          } 
	  Map<String,HttpDeviceInfo> map = new HashMap<String,HttpDeviceInfo>() ;
	  
	  for( int i = 0 ; i < infos.length ; i++ ){
	  
	     map.put( infos[i].getDeviceName() ,  infos[i] ) ;
	     
	  }
	  
	  String deviceList = driver._getDeviceList( sid ) ;
	  
	  String [] deviceIDs = deviceList.split(",") ;
	  
	  String [] deviceName = new String[deviceIDs.length] ;
	  String [] deviceMode = new String[deviceIDs.length] ;
	  
	  for( int i = 0 ; i < deviceName.length ; i++ ){
	  
	      deviceName[i] = driver._getDeviceName( sid , deviceIDs[i] ) ;
	      deviceMode[i] = driver._setDevice( sid , deviceIDs[i] , "?" ) ;
	      
	      HttpDeviceInfo info = map.get( deviceName[i]  ) ;
	      
	      if( info != null ){
	      
	           info.setDeviceId( deviceIDs[i] ) ;
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
	  
	      System.out.println( driver._setDevice( sid , aid , "1" ) ) ;
	      
	  }else if( mode.equals("off") ){

	      System.out.println( driver._setDevice( sid , aid , "0" ) ) ;
	  
	  }else if( mode.equals("query") ) {

	      System.out.println( driver._setDevice( sid , aid , "?" ) ) ;
	  
	  }else{
	      System.out.println(  "Unkown command" );
	  } 
     
*/ 
      }else if( command.equals("set") ){
      
          if( args.length < 5 ){
            System.out.println("Usage : ... <serverURL> set <sid> <ain> <value>");
            System.exit(1);
          }
          String sid  = args[2] ;
	  String aid  = args[3] ;
	  String mode = args[4] ;

          driver._setDevice( sid , aid , mode ) ;

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
}

