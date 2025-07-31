package org.rosi.drivers.http ;

import java.security.* ;
import java.nio.*;
import java.net.*;
import javax.net.ssl.* ;
import java.io.*;
import java.util.* ;
import org.rosi.drivers.generic.XGenericDriver ;

public class HttpSimpleDriverV2 implements XGenericDriver {

   private String _baseURL    = null ;
      
   public HttpSimpleDriverV2( String serverURL ){
      this._baseURL    = serverURL ;
   }
   /**
     *
     * -----------------------
    **/
    
   private InputStream getInputStream( String urlString ) throws Exception {
   
       URL url = new URI( urlString ).toURL() ;
       
       URLConnection connection = url.openConnection() ;
       
       if( ! ( connection instanceof HttpURLConnection ) )
           throw new
            Exception("Internal Error: connection not an HttpURLConnection" );
       
       HttpURLConnection http = (HttpURLConnection) connection ;
       
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
   /**
     *
     */
   public HttpDeviceInfo [] getDeviceInfoList() throws Exception {
   /* ---------------------------------------------------------------------- */
   
      List<HttpDeviceInfo> list = new ArrayList<HttpDeviceInfo>() ;
 
          String listString = getDeviceList( )  ;
          String [] deviceList = listString.split(";");
          for( int i = 0 ; i < deviceList.length ; i++ ){
             String deviceDetail = deviceList[i].trim() ;
             String [] allids = deviceDetail.split(",") ;
             String id     = allids[0].trim() ;
             if( id.equals("") )continue ;
             try{

                list.add( getDeviceInfo(  id ) ) ;

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
      //System.out.println("Sending : "+requestURL);
      fetchString( requestURL ) ;
   }
   public Map<String,String> getDeviceAttributes( String deviceName ) throws Exception {
        HttpDeviceInfo     info = getDeviceInfo( deviceName ) ;
        Map<String,String> map  = new HashMap<>();
        for( HttpDeviceInfo.Attribute attr : info.getAttributes() ){
           map.put( attr.getName() , attr.getValue() ) ;
        }
        return map;
   }
   /**
     *
     */
   public HttpDeviceInfo getDeviceInfo( String deviceId ) throws Exception {
   /* ---------------------------------------------------------------------- */

      String requestURL = _baseURL + "/info/" + deviceId ;
           
      String text = fetchString( requestURL ) ;
      HttpDeviceInfo info = HttpDeviceInfo.parsePlain( text ) ;
            
      return info ;
   }
   /**
     *
     */
   public void update() throws Exception {
       getDeviceNames();
   }
   public List<String> getDeviceNames() throws Exception {

       String list  = getDeviceList() ;
       String [] al = list.split(";");
       List<String> outList = new ArrayList<>();

       for( int i = 0 ; i < al.length ; i++ ){
          String [] el = al[i].split(",") ;
          String str = el[0].trim(); 
          if( str.length() > 0 )outList.add( str  ) ;
       }
       return outList ;
   }
   public String getDeviceList() throws Exception {
   /* ---------------------------------------------------------------------- */
   
      String requestURL = _baseURL + "/listdevices" ;
           
      String doc = fetchString( requestURL ) ;
            
      return doc ;
   }
   /**
     *
     */
   public void setDeviceAttribute( String deviceName , String attributeName ,  String value ) throws Exception {

       HttpDeviceInfo info = getDeviceInfo( deviceName ) ;

       HttpDeviceInfo.Attribute attr = info.getAttribute( attributeName ) ;

       attr.setValue( value  );

       setDeviceInfo( info ) ;
   
       return ;
   }
   public void setDevice( String ain , String attributeName ,  String mode ) throws Exception {
   /* ---------------------------------------------------------------------- */

       String [] a = attributeName.split("\\.") ;
       if( a.length != 2 )
          throw new
          IllegalArgumentException("AtributeName : <deviceId>.<attributeName> "+attributeName+" "+a.length);

       HttpDeviceInfo info = getDeviceInfo( a[0] ) ;

       HttpDeviceInfo.Attribute attr = info.getAttribute(a[1]) ;

       attr.setValue( mode  );

       setDeviceInfo( info ) ;
   
       return ;
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
        System.out.println("-- Generics --");
        System.out.println("Usage : ... <serverURL> getdevicenames");
        System.out.println("Usage : ... <serverURL> getattributes <deviceName>");
        System.out.println("Usage : ... <serverURL> setattribute <deviceName> <attrName> <value>");
        System.out.println("-- Specific --");
        System.out.println("Usage : ... <serverURL> getdevicelist");
        System.out.println("Usage : ... <serverURL> getdeviceinfo <deviceId>");
        System.exit(1);
      }

 
      try{
         runMain( args ) ;
      }catch(Exception ee ){
         System.err.println("Exception : ("+ee.getMessage()+")");
         ee.printStackTrace();

         System.exit(4);
      }
 
      System.exit(0);
 }
 public static void runMain( String [] args ) throws Exception {

      String urlString      = args[0] ;
      String command        = args[1] ;
      HttpSimpleDriverV2 driver = new HttpSimpleDriverV2( urlString ) ;
       
      if( command.equals("getdeviceinfolist") ){
      
          if( args.length < 2 ){
            System.out.println("Usage : ... <serverURL> getdeviceinfolist");
            System.exit(1);
          }
	  
	  HttpDeviceInfo [] infos = driver.getDeviceInfoList() ;
	  
	  for( HttpDeviceInfo info : infos ){
	     System.out.println( info.toString() ) ;
	  }
     
      }else if( command.equals("getdeviceinfo") ){
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> getdeviceinfo <deviceID>");
            System.exit(1);
          }
          String deviceId = args[2] ;

	  HttpDeviceInfo info =  driver.getDeviceInfo( deviceId )  ;

          System.out.println(printDeviceInfo(info));
      
      }else if( command.equals("getattributes") ){

          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> getattributes <deviceName>");
            System.exit(1);
          }
          XGenericDriver gen = (XGenericDriver)driver  ;
          String deviceID = args[2] ;

          for( Map.Entry<String,String> e : gen.getDeviceAttributes(deviceID).entrySet() ){
              System.out.println( e.getKey() + " -> "+e.getValue() );
          }
      }else if( command.equals("getdevicenames") ){
         XGenericDriver gen = (XGenericDriver)driver  ;
         for( String name : gen.getDeviceNames() ){
            System.out.println(" -> "+name);
         }
      }else if( command.equals("getdevicelist") ){
      
          if( args.length < 2 ){
            System.out.println("Usage : ... <serverURL> getdevicelist");
            System.exit(1);
          }
	  System.out.println( driver.getDeviceList()  );
     
      }else if( command.equals("ls") ){
          if( args.length < 2 ){
            System.out.println("Usage : ... <serverURL> ls");
            System.exit(1);
          }
          String listString = driver.getDeviceList()  ;
          String [] deviceList = listString.split(";");
          for( int i = 0 ; i < deviceList.length ; i++ ){
             String deviceDetail = deviceList[i].trim() ;
             String [] allids = deviceDetail.split(",") ;
             String id     = allids[0].trim() ; 
             if( id.equals("") )continue ;
                try{
	           HttpDeviceInfo info =  driver.getDeviceInfo( id )  ;
                   System.out.println(printDeviceInfo( info ));
                }catch(Exception ee ){
                   System.out.println("Server problem with ID : "+id ) ;
                }
 
          }
      }else if( command.equals("setattribute") ){
      
          if( args.length < 5 ){
            System.out.println("Usage : ... <serverURL> setattribute <deviceName> <attribute> <value>");
            System.exit(1);
          }
          XGenericDriver gen = (XGenericDriver)driver  ;
	  String device  = args[2] ;
	  String attr    = args[3] ;
	  String value   = args[4] ;

          gen.setDeviceAttribute( device , attr, value ) ;

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
