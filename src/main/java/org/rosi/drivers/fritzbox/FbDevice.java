package org.rosi.drivers.fritzbox ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public class FbDevice {

   private String _name            = null ;
   private String _identifier      = null ;
   private int    _state           = -1 ;
   private String _device          = null ;
   private float  _temperature     = (float)0.0 ;
   private float  _power           = (float)0.0 ;
   private String _vendorDetails   = null ;
   private int    _functionBitMask = 0 ;
   private Map<String,String> _map = new HashMap<String,String>();

   private static final int FUNC_HANFUN_Device      = ( 1 << 0 ) ;
   private static final int FUNC_Alarm_Sensor       = ( 1 << 4 ) ;
   private static final int FUNC_Heater_Actor       = ( 1 << 6 ) ;
   private static final int FUNC_Energy_Sensor      = ( 1 << 7 ) ;
   private static final int FUNC_Temperature_Sensor = ( 1 << 8 ) ;
   private static final int FUNC_Switch_Actor       = ( 1 << 9 ) ;
   private static final int FUNC_AVM_DECT_Repeater  = ( 1 << 10 ) ;
   private static final int FUNC_Microfon_Sensor    = ( 1 << 11 ) ;
   private static final int FUNC_HANFUN_Unit        = ( 1 << 13 ) ;

   enum DeviceFuntion {
       Device_HANFUN , Sensor_Alarm , Actor_Heater , Sensor_Energy ,
       Sensor_Temperature , Actor_Switch , Repeater_AVM_DECT ,
       Sensor_Microfon , Unit_HANFUN 
   }
   public FbDevice( Node node ){
      if( ! node.getNodeName().equals("device") )
         throw new
         IllegalArgumentException("Node name not 'device'");

      NodeEntry nodeEntry = new NodeEntry( node ) ;
      processDeviceNode( nodeEntry ) ;
   }
   public String toInfoLine(){
      StringBuffer sb = new StringBuffer() ;
      sb.append(getName()).append("[").append(getDeviceID()).append(";").append(getVendorDetails()).append("]={");

      if( getState() == 0 ){
          sb.append("DISCON");
      }else{
          if( ( _functionBitMask & FUNC_Energy_Sensor ) > 0 ){
              String st = _map.get("powermeter.energy");
              sb.append("E=").append(st==null?"?":st).append(";");
          }    
          if( ( _functionBitMask & FUNC_Temperature_Sensor ) > 0 ){
              String st = _map.get("temperature.celsius");
              sb.append("T=").append(st==null?"?":st).append(";");
          }
          if( ( _functionBitMask & FUNC_Switch_Actor ) > 0 ){
              String st = _map.get("switch.state");
              sb.append("S=").append(st==null?"?":st).append(";");
          }
          if( ( _functionBitMask & FUNC_Heater_Actor ) > 0 ){
             String s = null ;
             sb.append("H={");
             sb.append("i=").append(getSmart("hkr.tist")).append(";") ;
             sb.append("s=").append(getSmart("hkr.tsoll")).append(";") ;
             sb.append("l=").append(getSmart("hkr.absenk")).append(";") ;
             sb.append("h=").append(getSmart("hkr.komfort")).append(";") ;
             sb.append("E=").append(getSmart("hkr.errorcode")).append(";") ;
             sb.append("B=").append(getSmart("hkr.batterylow")).append(";") ;
             sb.append("}");
          }
      }
      sb.append("}");
      return sb.toString();
   }
   private String getSmart( String key ){
      String s = _map.get(key);
      return s == null ? "?" : s ; 
   }
   private void processDeviceNode( NodeEntry nodeEntry ){

      Map<String,String> map = _map ;
      //System.out.println(nodeEntry.toString());
      String vendor  = nodeEntry.getAttribute("manufacturer");
      String version = nodeEntry.getAttribute("fwversion");
      String id      = nodeEntry.getAttribute("id");
      String product = nodeEntry.getAttribute("productname");

      String bitMask = nodeEntry.getAttribute("functionbitmask" ) ;
      /*
       * The function bits
       * -------------------------------
       */
      _functionBitMask = 0 ;
      if( bitMask != null ){
         try{
            _functionBitMask = Integer.parseInt( bitMask ) ; 
         }catch(Exception ee ){ }
      }
      /*
       * The function vendor string.
       * -------------------------------
       */
      StringBuffer sb = new StringBuffer() ;
      sb.append(vendor==null?"":vendor).append(",").
         append(product==null?"":product).append(",").
         append(version==null?"":version).append(",").
         append(id==null?"ID":id) ;

      map.put( "vendor"          , _vendorDetails = sb.toString() );
      map.put( "identifier"      , _device        = nodeEntry.getAttribute("identifier") );
      map.put( "functionbitmask" , bitMask  );

      collectNode( nodeEntry , null , map );

      for( Map.Entry<String,String> e : map.entrySet() ){
         String name  = e.getKey() ;
         String value = e.getValue() ;
         try{
            fixValues( name , value , map ) ;
         }catch(Exception ee ){
            System.out.println("Conversion error : "+name+":"+value);
         }
      }
   }
   private void fixValues( String name , String value , Map<String,String> map ){
         if( name.equals("name") ){
            _name = value ;
         }else if( name.equals("present") ){
            _state = Integer.parseInt(value);
         }else if( name.equals("power") ){
            _power = ((float)Integer.parseInt( value )) / (float)1000.0 ;
         }else if( name.equals("hkr.tist" ) ){
            map.put( name , ""+( Float.parseFloat( value ) / (float)2.0 ) ) ;
         }else if( name.equals("hkr.nextchange.tchange" ) ){
            map.put( name , ""+( Float.parseFloat( value ) / (float)2.0 ) ) ;
         }else if( name.equals("hkr.tsoll" ) ){
            map.put( name , ""+( Float.parseFloat( value ) / (float)2.0 ) ) ;
         }else if( name.equals("hkr.absenk" ) ){
            map.put( name , ""+( Float.parseFloat( value ) / (float)2.0 ) ) ;
         }else if( name.equals("hkr.komfort" ) ){
            map.put( name , ""+( Float.parseFloat( value ) / (float)2.0 ) ) ;
         }else if( name.equals("powermeter.energy" ) ){
            map.put( name , ""+( Float.parseFloat( value ) / (float)100.0 ) ) ;
         }else if( name.equals("temperature.offset" ) ){
            map.put( name , ""+( Float.parseFloat( value ) / (float)10.0 ) ) ;
         }else if( name.equals("temperature.celsius" ) ){
            _temperature = Float.parseFloat( value ) / (float)10.0 ;
            map.put( name , ""+_temperature) ;
         }
         
   } 
   public boolean isAlarmSensor(){        return ( _functionBitMask & FUNC_Alarm_Sensor ) != 0 ; }
   public boolean isHeaterActor(){        return ( _functionBitMask & FUNC_Heater_Actor ) != 0 ; }
   public boolean isEnergySensor(){       return ( _functionBitMask & FUNC_Energy_Sensor ) != 0 ; }
   public boolean isTemperature_Sensor(){ return ( _functionBitMask & FUNC_Temperature_Sensor ) != 0 ; }
   public boolean isSwitchActor(){        return ( _functionBitMask & FUNC_Switch_Actor ) != 0 ; }

   private void collectNode( NodeEntry nodeEntry , String n , Map<String,String> map ){
      for( NodeEntry e : nodeEntry.getValues() ){
         String name = ( n == null ? "" : (n+".") ) + e.getNodeType() ;
         String value = e.getValue() ;
         if( value != null ){
            map.put( name , value ) ;
         }else{
            collectNode( e , name , map );
         } 
      }
   }
   public Map<String,String> getMap(){ return _map ; }
   public String getVendorDetails(){ return _vendorDetails ; }
   public int getState(){ return _state ; }
   public String getDeviceID(){ return _device ; }
   public float getTemperature(){ return _temperature ; }
   public float getPower(){ return _power ; }
   public String getName(){ return _name ; }

  public String toString(){
      StringBuffer sb = new StringBuffer() ;
      printDevice(sb);
      return sb.toString();
  }
  public void printDevice( StringBuffer sb ){
     for( Map.Entry<String,String> e : _map.entrySet() ){
         sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
     }
  }

}

