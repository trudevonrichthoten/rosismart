package org.rosi.modules.engine.nodes ;

public class RosiValue implements Cloneable, Comparable<RosiValue> {


   public String formatString( String gap ){
   
       return gap+ getValueAsString() + " ("+getNodeClassType()+")\n" ;
       
   }
   public RosiValue clone(){
    throw new
    IllegalArgumentException("Can't be coned");
   }
   public String getValueType(){ return "!" ; }
   public String getRosiType(){ return "?" ; }
   public String toString(){
       return  getValueAsString()+"  ("+getNodeClassType()+")" ;
   }
   public String getValueAsString(){
      throw new
      IllegalArgumentException("Can't convert "+getNodeClassType()+" to 'String'");
   }
   public String getNodeClassType(){
       String [] x = getClass().getName().split("\\.") ;
       return x[x.length-1] ;
   }
   public float getValueAsFloat(){
      throw new
      IllegalArgumentException("Can't convert "+getNodeClassType()+" to 'Float'");
      // return Float.parseFloat(getValueAsString());
   }
   public boolean getValueAsBoolean(){
      throw new
      IllegalArgumentException("Can't convert "+getNodeClassType()+" to 'Boolean'");
      // return Boolean.parseBoolean(getValueAsString());
   }
   public long getValueAsNumber(){
      throw new
      IllegalArgumentException("Can't convert "+getNodeClassType()+" to 'Number'");
      // return Integer.parseInt(getValueAsString()) ;
   }

   public int compareTo(RosiValue other ){
       return this.getValueAsString().compareTo(other.getValueAsString()) ;
   }
 
}
