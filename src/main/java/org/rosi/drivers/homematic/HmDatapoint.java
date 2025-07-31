package org.rosi.drivers.homematic ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public class HmDatapoint extends HmNodeEntry {

   public HmDatapoint( Node node ){
     super(node);
   }
   public XNodeEntry getXNodeEntryChild( Node node ){
      return null ;
   }
   public String getName(){ 
      return getAttribute("type");      
   }
   public String getValue(){
      return getAttribute("value");      
   }   
   public String toString(){
      StringBuffer sb = new StringBuffer() ;
      sb.append(getName()).append("={ID=").append(getId()).append(";Value=").append(getValue()).append("}");
      return sb.toString();
   }

}
