package org.rosi.drivers.homematic ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public class HmChannel extends HmNodeEntry {

   public HmChannel( Node node ){
     super(node);
   }
   public XNodeEntry getXNodeEntryChild( Node node ){
      String name = node.getNodeName() ;
      if(  name.equals("datapoint") )return new HmDatapoint(node) ;
      return null ;
   }
   @SuppressWarnings("unchecked")
   public List<HmDatapoint> datapoints(){
      return (List<HmDatapoint>)(List<?>)_getValues() ;
   }

}
