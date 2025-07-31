package org.rosi.drivers.homematic ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public class ChannelNodeEntry extends XNodeEntry {

   public ChannelNodeEntry( Node node ){
     super(node);
   }
   public XNodeEntry getXNodeEntryChild( Node node ){
       return new DataPointNodeEntry( node );
   }
}
