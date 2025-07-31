package org.rosi.drivers.fritzbox ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Arrays;
import org.rosi.drivers.homematic.XNodeEntry;

public class NodeEntry extends XNodeEntry {

   public NodeEntry( Node node ){
     super(node);
   }
   public XNodeEntry getXNodeEntryChild( Node node ){
      return new NodeEntry(node);
   }
   public String getName(){ return super.getNodeType() ; }
   public String getValue(){ return super.getXNodeValue() ; }
   @SuppressWarnings("unchecked")
   public List<NodeEntry> getValues() {
      /*
      return Arrays.asList( _getValues().toArray( new NodeEntry[0] ) ) ;
      */
      return (List<NodeEntry>)(List<?>)_getValues() ;
   }
  
}
