package org.rosi.drivers.homematic ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public abstract class HmNodeEntry extends XNodeEntry {

   public HmNodeEntry( Node node ){
     super(node);
   }
   public String getName(){
      return getAttribute("name");
   }
   public int getId(){
      return Integer.parseInt( getAttribute("ise_id") ) ;
   }
   public long getTimestampe(){
      return Long.parseLong( getAttribute("timestamp") ) ;
   }
}
