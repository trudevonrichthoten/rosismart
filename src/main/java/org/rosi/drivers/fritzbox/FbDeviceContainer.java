package org.rosi.drivers.fritzbox ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Iterator ;

public class FbDeviceContainer {

   private List<FbDevice> _infoList = new ArrayList<FbDevice>() ;
   private Map<String,FbDevice> _infoMap = new HashMap<String,FbDevice>() ;

   public FbDeviceContainer( Node node ){
      if( ! node.getNodeName().equals("devicelist") )
         throw new
         IllegalArgumentException("Node name not 'device'");
      _scanDevices( node ) ;
   }
   public FbDevice getDeviceByID( String deviceId ){
      return _infoMap.get(deviceId);
   }
   public String toString(){
      StringBuffer sb = new StringBuffer() ;
      printContainer(sb) ;
      return sb.toString();
   }
   public List<FbDevice> devices(){
      return _infoList ;
   }
   public List<String> getDeviceNames(){
      List<String> list = new ArrayList<>();
      for( FbDevice device : _infoList ){
          list.add( device.getDeviceID().replace(" ","") ) ;
      }
      return list ;
   }
   public void printContainer( StringBuffer sb ){
      for( FbDevice device : _infoMap.values() ){
          sb.append("- Name : ").append(device.getName()).append(" (").
             append(device.getDeviceID()).append(")\n");
          device.printDevice(sb);
      }
   }
   private void _scanDevices( Node node ){

       NodeList deviceList = node.getChildNodes() ;

       for( int j = 0 ; j < deviceList.getLength() ; j++ ){

          FbDevice device = new FbDevice( deviceList.item(j) ) ;
           _infoList.add( device ) ;
            //System.out.println("Device ID : >"+device.getDeviceID()+"<");
          String name = device.getDeviceID() ;
           _infoMap.put( name , device ) ;
           _infoMap.put( name.replace(" ","") , device ) ;

       }
   }
}

