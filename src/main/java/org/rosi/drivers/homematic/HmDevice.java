package org.rosi.drivers.homematic ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public class HmDevice extends HmNodeEntry {
   private String _name  = null ;
   private String _type  = null ; 
   private Map<String,HmDatapoint> _map = new HashMap<>();
   private Map<String,String>     _xmap = new HashMap<>() ;
   public HmDevice( Node node ){
     super(node);
     processDatapoints();
   }
   public Map<String,HmDatapoint> getDataMap() {
      return _map ; 
   }
   public Map<String,String> getMap() {
      return _xmap ; 
   }
   public String getName(){
      if( _name == null ){
         String [] name = super.getName().split(" ") ;
         if( name.length > 1 ){
            _name = name[1] ;
            _type = name[0] ; 
         }else{
            _name = name[0] ;
            _type = "unknown" ; 
         }
      }
      return _name ; 
   }
   public String getType(){
      if( _type == null )getName() ;
      return _type ;
   }
   public XNodeEntry getXNodeEntryChild( Node node ){
      String name = node.getNodeName() ;
      if(  name.equals("channel") )return new HmChannel(node) ;
      return null ;
   }
   private void processDatapoints(){
      for( HmChannel channel : channels() ){
         for( HmDatapoint datapoint : channel.datapoints() ){
             _map.put( datapoint.getName() , datapoint ) ;
         }
      } 
      /*  
       * Fix for convenience.
       */
      String [] [] _mapping =  {
        { "set_temperature" , "SET_TEMPERATURE" } ,
        { "temperature" , "ACTUAL_TEMPERATURE" } ,
        { "battery"     , "BATTERY_STATE" },
        { "battery_low" , "LOWBAT" } ,
        { "unreachable" , "UNREACH" } ,
        { "rssi_peer"   , "RSSI_PEER" } ,
        { "rssi_device" , "RSSI_DEVICE" }  ,
        { "valve"       , "VALVE_STATE" } 
      };
     
      for( int i = 0 ; i < _mapping.length ; i++ ){
         HmDatapoint dp = _map.get( _mapping[i][1] ) ;
         if( dp != null ) _xmap.put( _mapping[i][0] ,dp.getValue());
      }
   }
   public String toInfoLine(){
      return "";   
   }
   @SuppressWarnings("unchecked")
   public List<HmChannel> channels(){
      return (List<HmChannel>)(List<?>)_getValues() ;
   }

}
