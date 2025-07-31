package org.rosi.modules.engine.nodes ;

public class RosiConstant extends RosiValue {

   public String getValueType(){ return "C" ; }
   public String getRosiType(){ return "C" ; }

   public static RosiConstant parseConstant( String value ){
      if( value == null || value.isEmpty() ) return new StringValue(value) ;
      if( value.equalsIgnoreCase("true" ) ||
            value.equalsIgnoreCase("false")   )return new BooleanValue(value) ;

      try{
         return new NumberValue( Integer.parseInt(value) ) ;
      }catch(NumberFormatException ignore){}
      try{
         return new FloatValue( Double.parseDouble(value) ) ;
      }catch(NumberFormatException ignore){}

      return new StringValue(value);
   }

}
