package org.rosi.drivers.homematic ;

import java.security.* ;
import java.nio.*;
import java.net.*;
import javax.net.ssl.* ;
import java.io.*;
import java.util.* ;
import javax.xml.parsers.*;
import org.w3c.dom.*;

import org.rosi.drivers.generic.* ;

public class HmDriver implements XGenericDriver {

   private String _baseURL        = null ;
   private String _stateListURI   = null ;
   private String _deviceListURI  = null ;
   private String _stateChangeURI = null ;
      
   private HmDeviceContainer _container =  null ;

   public HmDriver( String serverURL ){

      this._baseURL        = serverURL ;
      this._stateListURI   = "/addons/xmlapi/statelist.cgi" ;
      this._deviceListURI  = "/addons/xmlapi/devicelist.cgi" ;
      this._stateChangeURI = "/addons/xmlapi/statechange.cgi" ;

   }
   /**
     *
     *  Fetch the XML document
     * -----------------------
    **/
    
   private static InputStream getInputStream( String urlString ) throws Exception {
   
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
   public static Node requestXmlDocument(  String urlString ) throws Exception {

       InputStream inStream = getInputStream( urlString ) ;

       try{
       
           DocumentBuilder docbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder() ;

           Document doc = docbuilder.parse( inStream ) ;

           return doc.getFirstChild() ;

       }catch(Exception ioe ){
           throw ioe ;
       }finally{
           try{ inStream.close() ; }catch(IOException ioe ){}
       }
        

   }
   public String fetchString(  String urlString ) throws Exception {

       InputStream inStream = getInputStream( urlString ) ;
       try{
           BufferedReader br = new BufferedReader( new InputStreamReader( inStream ) ) ;             
       
           String line = null ;
           StringBuffer sb = new StringBuffer() ;
           while( ( line = br.readLine() ) != null )sb.append(line) ;
           String responds = sb.toString() ;
           String [] tokens = responds.split(" ");
           if( responds.startsWith("HTTP/1.0") ){
               throw 
               new IllegalArgumentException("Server reported : "+tokens[1]);
           }
           return  responds ;
       }catch(Exception ee ){
           throw ee ;
       }finally{
           try{ inStream.close() ; }catch(IOException ioe ){}
       }

   } 
   public synchronized void update() throws Exception {
       _container = getDeviceContainer() ;
   } 
   public synchronized List<String> getDeviceNames()  throws Exception {
       if( _container == null )update() ; 
       List<String> list = new ArrayList<>();
       for( HmDevice device : _container.devices() ){
          list.add( device.getName() );
       } 
       return list ;
   }
   public synchronized Map<String,String> getDeviceAttributes( String deviceName ) throws Exception{
       if( _container == null )update() ; 
       HmDevice device = _container.getDeviceByName( deviceName ) ;
       if( device == null )
          throw new
          IllegalArgumentException("Device not found '"+deviceName+"'") ;

       return device.getMap() ;
   }
   public HmDeviceContainer  getDeviceContainer() throws Exception {

      String requestURL = _baseURL + _stateListURI ;
      
      Node node = requestXmlDocument( requestURL ) ;
      
      HmDeviceContainer container =  new HmDeviceContainer(node) ;

      return container ;
   }
   public synchronized void setDeviceAttribute( String deviceName , String key , String value ) throws Exception {
 
      if( _container == null )update() ; 

      HmDevice device = _container.getDeviceByName( deviceName ) ;
      if( device == null )
        throw new
        IllegalArgumentException("Device not found : "+deviceName);

      key = key.equals("temperature") ? "SET_TEMPERATURE" : key ;

      Map<String,HmDatapoint> map = device.getDataMap() ;
      HmDatapoint dp = map.get(key);
      if( dp == null )
        throw new
        IllegalArgumentException("Device "+deviceName+" doesn't support "+key);

      String requestURL = _baseURL +
                          _stateChangeURI + "?" + 
                          "ise_id="+dp.getId()+"&new_value="+value;
           
      requestXmlDocument( requestURL ) ;
   }
   public static void main( String [] args )throws Exception {

      if( args.length < 2 ){
        System.out.println("Usage : ... <serverURL> getdevicenames");
        System.out.println("Usage : ... <serverURL> getattributes <deviceName>");
        System.out.println("Usage : ... <serverURL> setattribute <deviceName> <key> <value>");
        System.out.println("Usage : ... <serverURL> ls");
        System.exit(1);
      }

      String urlString      = args[0] ;
      String command        = args[1] ;

      HmDriver       driver  = new HmDriver( urlString ) ;
      XGenericDriver generic = driver ;

     
      if( command.equals("getdevicecontainer") ){
      
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> ls <sid>");
            System.exit(1);
          }
          String sid = args[2] ;

	  HmDeviceContainer devices = driver.getDeviceContainer() ;
          System.out.println(devices.toString());

      }else if( command.equals("ls") ){
      
          if( args.length < 2 ){
            System.out.println("Usage : ... <serverURL> ls [<sid>]");
            System.exit(1);
          }
          String sid = args.length < 3 ? "00000000" : args[2]  ;

	  Map<String,HmDevice> map = new HashMap<String,HmDevice>() ;

          HmDeviceContainer container = driver.getDeviceContainer() ;

          for( HmDevice device : container.devices() ){
             System.out.println("Device : "+device.getName()+" ["+device.getId()+"]");
             for( HmChannel channel : device.channels() ){
                System.out.println("   Channel : "+channel.getName()+" ["+channel.getId()+"]");
                for( HmDatapoint datapoint : channel.datapoints() ){
                   System.out.println("      Datapoint : "+datapoint.getName()+" ["+datapoint.getId()+"]");
                }
             }
          }

          System.out.println("--------- Data Map -----------");
          for( HmDevice device : container.devices() ){
             System.out.println("Device : "+device.getName()+" ["+device.getId()+"]");
             for( Map.Entry<String,HmDatapoint> e : device.getDataMap().entrySet() ){
                System.out.println("    "+e.getKey()+" : "+e.getValue());
             }
          }
     
      }else if( command.equals("getdevicenames") ){
      
          List<String> names = driver.getDeviceNames() ;
          for( String name : names ){
            System.out.println(name);
          }
      }else if( command.equals("setattribute") ){
      
          if( args.length < 5 ){
            System.out.println("Usage : ... <serverURL> setattribute <device> <key> <value>");
            System.exit(1);
          }
	  String aid   = args[2] ;
	  String key   = args[3] ;
          String value = args[4] ;
	  
	  driver.setDeviceAttribute(  aid , key , value )  ;
	  
      }else if( command.equals("getattributes") ){
      
          if( args.length < 3 ){
            System.out.println("Usage : ... <serverURL> getattributes <deviceName>");
            System.exit(1);
          }
	  String deviceID = args[2] ;

          for( Map.Entry<String,String> e : generic.getDeviceAttributes(deviceID).entrySet() ){
	      System.out.println( e.getKey() + " -> "+e.getValue() );
          }
      }
   }
}

