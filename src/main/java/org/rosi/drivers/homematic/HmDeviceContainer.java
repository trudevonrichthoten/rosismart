package org.rosi.drivers.homematic ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public class HmDeviceContainer extends HmNodeEntry {
   private Map<String,HmDevice> _deviceMap = new HashMap<>();  
   public HmDeviceContainer( Node node ){
     super(node);
     for( HmDevice device : devices() ){
        _deviceMap.put( device.getName() , device ) ;
     }
   }
   public HmDevice getDeviceByName( String name ){
      return _deviceMap.get(name);
   }
   public XNodeEntry getXNodeEntryChild( Node node ){
       String name = node.getNodeName() ;
       if(  name.equals("device")  )return new HmDevice( node ) ;
       return null; 
   }
   @SuppressWarnings("unchecked")
   public List<HmDevice> devices(){
      return (List<HmDevice>)(List<?>)_getValues() ;
   }
}
