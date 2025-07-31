package org.rosi.modules.engine.nodes ;

 

 public class StringValue extends RosiConstant {
 
    private String _string = null  ;
    
    public StringValue( String f ){
       _string = f ;
    }
    public StringValue clone(){ return new StringValue(_string) ; }
    public String getValueType(){ return "L" ; }
    public String getString(){ return _string ; }
    public void setString( String f ){ _string = f ; }
    public void setValue( String f ){ _string = f ; }
    public String getValueAsString(){ return _string ; }

 }

