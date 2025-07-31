package org.rosi.drivers.homematic ;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;

public abstract class XNodeEntry {

   private String _name  = null ;
   private String _value = null ;
   private List<XNodeEntry>   _values = new ArrayList<>();
   private Map<String,String> _attr   = new HashMap<String,String>();

   public XNodeEntry( Node deviceNode ){
      _name = deviceNode.getNodeName() ;
      scanDeviceNode( this , deviceNode ) ;
   }
   public List<XNodeEntry> _getValues(){ return _values ; }
   public String getNodeType(){ return _name ; }
   public String getXNodeValue(){ return _value ; }
   public String getAttribute(String key ){
     return _attr.get(key);
   }

   public XNodeEntry scanDeviceNode( XNodeEntry entry , Node deviceNode ){

      //if( nodeName.equals("#text") )return null ;
      String v =  deviceNode.getNodeValue() ;
      if( v != null ){
          entry._value = v ;
          return entry ;
      }

      NamedNodeMap map = deviceNode.getAttributes() ;
      if( map != null ){
          for( int i = 0 ; i < map.getLength() ; i++ ){
             Node   n     = map.item(i);
             String value = n.getNodeValue() ; // getChildValue(n) ;
             entry._attr.put( n.getNodeName()  , value == null ? "$EMPTY$" : value ) ;
          }
      }
      NodeList list = deviceNode.getChildNodes() ;
      for( int i = 0 ; i < list.getLength() ; i++ ){
         Node   n     = list.item(i) ;
         XNodeEntry childNodeEntry = getXNodeEntryChild( n );

         if( childNodeEntry != null )entry._values.add( childNodeEntry  ) ;
      }
      if( entry._values.size() == 1  ){
         XNodeEntry e0 =  entry._values.get(0) ; 
         if( e0.getNodeType().equals("#text") ){
            entry._value = e0._value ;
         }
      }

      return entry ;
  }
  public abstract XNodeEntry getXNodeEntryChild( Node node ) ;
  public String toString(){
     StringBuffer sb = new StringBuffer() ;
     printDevice( sb , this , "  " ) ;
     return sb.toString() ;
  }
  public static void printDevice( StringBuffer sb , XNodeEntry node , String gap ){
     sb.append(gap).append("struct = ").append(node.getNodeType()) ;
     if( node._value != null )sb.append(" = ").append(node._value) ;
     sb.append("\n");
     gap = gap + "    " ;
     for( Map.Entry<String,String> e : node._attr.entrySet() ){
        sb.append(gap).append("attribute.").
           append(e.getKey()).append(" = ").
           append(e.getValue()).append("\n") ;
     }  
     for( XNodeEntry ee : node._values ){
        String    v  = ee.getXNodeValue() ;
//        sb.append(gap).append("value.").append(ee.NodeType());
        if( v != null ){
           sb.append(" = ").append(v).append("\n") ;
        }else{
//           sb.append("\n");
           printDevice( sb , ee , gap ) ;
        }
     }  
  }

}

