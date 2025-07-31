package org.rosi.modules.engine.nodes ;

import java.util.*;

public class RosiArray extends RosiValue {
 
   private Map<String,RosiConstant> _map = new HashMap<String,RosiConstant>() ;

   public void put( String indexName , RosiConstant value ){
     _map.put( indexName , value ) ;
   }
   public RosiConstant get(String indexName ) {
     return _map.get(indexName);
   }
   public int size(){ return _map.size() ; }
   public Map<String,RosiConstant> getMap(){ return _map ; }

   public String formatString( String gap ){

        StringBuffer sb = new StringBuffer() ;

        sb.append( gap ).append("Array") ;

        for( Map.Entry<String,RosiConstant> e : _map.entrySet() ){
//           sb.append( gap ).append("  A : ").append(e.toString());
           sb.append( gap ).append("  A : ").append(e.getKey()).
              append(" = ").append( e.getValue() ).append("\n") ;
        }

        return sb.toString() ;
   }


}
