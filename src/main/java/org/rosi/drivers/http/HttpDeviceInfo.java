package org.rosi.drivers.http ;

import java.util.* ;
import java.io.*;
   
public class HttpDeviceInfo {
/*
   private String  _name    = null ;
   private String  _device  = null ;
*/
   private int     _state   = -1 ;
   private boolean _present = false ;

   private String _deviceName = "?" ;
   private String _deviceId   = null ;
   private String _deviceType = "?" ;
   private Map<String,Attribute> _map = new HashMap<String,Attribute>() ;

   public static class Attribute {
      private String _name = null ; 
      private String _id   = null ;
      private String _type = null ;
      private String _value  = null ;
      private String _mode  = null ;
      private boolean _modified = false ;
      private Attribute( String [] attr , String value ){
        _value = value.trim() ;
        _id    = attr[2] ;
        _name  = attr[1] ;
        _mode  = attr[3] ;
        _type  = attr[4] ;
      }
      public String getValue(){ return _value ; }
      public String getMode(){ return _mode ; }
      public String getName(){ return _name ; }
      public void clear(){ _modified = false ; }
      public boolean isModified(){ return _modified ; }
      public void setValue( String value )throws IllegalArgumentException {
         if( value == null )
            throw new
            IllegalArgumentException("Value must not be 'null'!"); 
         if( _mode == null )
            throw new
            IllegalArgumentException("Attribute not initialized!"); 
         if(  _mode.equals("r") )
            throw new
            IllegalArgumentException("Attribute not writeable!"); 
 
         _value    = value ;
         _modified = true ;
         
      }

   }
   public List<Attribute> getAttributes(){ return new ArrayList<Attribute>(_map.values() ); }
   public static HttpDeviceInfo parsePlain( String plainText ) throws IOException , IllegalArgumentException {
       HttpDeviceInfo info = new HttpDeviceInfo() ;

       BufferedReader br = new BufferedReader( new StringReader( plainText ) ) ;
       String line = null ;
       try{
           while( ( line = br.readLine() ) != null ){
              line = line.trim();
              if( line.startsWith("deviceId:") ){
                  info._deviceId = line.split(":")[1].trim() ;
              }else if( line.startsWith("deviceName:") ) {
                  info._deviceName = line.split(":")[1].trim();
              }else if( line.startsWith("deviceType:") ) {
                  info._deviceType = line.split(":")[1].trim() ;
              }else if( line.startsWith("attr,") ) {
                  String [] x = line.split(":") ;
                  if( x.length < 2 )continue ;
                  String [] y = x[0].split(",") ;
                  if( y.length < 5 )continue ; 
                  info._map.put( y[1].trim() , new Attribute( y , x[1] ) ) ;
              }
           }
       }finally{
          try{ br.close() ; }catch(Exception ee ){}
       } 
       if( info._deviceId == null )
           throw new
           IllegalArgumentException("Device Id not specified in server response!");

       return info ;
   }
   public HttpDeviceInfo(){
   }
   public HttpDeviceInfo( String id , String name ){
     _deviceId   = id ;
     _deviceName = name ;
   } 
   public Attribute getAttribute( String key ) throws IllegalArgumentException {
     Attribute result = _map.get(key);
     if( result == null )
       throw new
       IllegalArgumentException("Key not found : "+key);
     return result ;
   } 
 
   public void setDeviceId( String deviceId ){
     _deviceId = deviceId ;
   }
   public String getDeviceId(){ return _deviceId ; }
   public String getDeviceType(){ return _deviceType ; }
   public void setDeviceName( String name ){
       _deviceName = name ;
   }
   public String getDeviceName(){ return _deviceName ; }

   
   public String toString(){

      StringBuffer sb = new StringBuffer() ;
      
      sb.append( _deviceId ).
           append("={Name=").append(_deviceName).
           append(";Type=").append(_deviceType).append(";map={") ;
      for( Map.Entry<String,Attribute> e : _map.entrySet() ){
         sb.append( e.getKey() ).append("=").append(e.getValue().getValue()).append(";");
      } 
      sb.append("}}") ;
      return sb.toString() ;
   }
   public String toString2(){
      StringBuffer sb = new StringBuffer() ;
      sb.append( _deviceName ).append("(").append(_present?"OK":"DISCON") ;
      if( _deviceId != null )sb.append(",id=").append(_deviceId) ;
      if( _present ){
      	 sb.append(",status=").append( _state < 0 ? "?" : ( _state == 0 ? "off" : "on" ) ) ;
      }
      sb.append(")") ;
      return sb.toString() ;
   }

   public void setState( int state ){
      _state = state ;
   }
   public void setState( String state ){
      _state = state.equals("1") ? 1 : ( state.equals("0") ? 0 : -1 ) ;
   }
   public int getState(){ return _state ; }
   public void setPresent( boolean present ){ _present = present ;}
   public boolean isPresent(){ return _present ; }
}

