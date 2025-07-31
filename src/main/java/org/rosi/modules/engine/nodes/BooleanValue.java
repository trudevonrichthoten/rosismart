package org.rosi.modules.engine.nodes ;

 
 public class BooleanValue extends RosiConstant {
 
    private boolean _boolean = false ;
    public BooleanValue( boolean f ){  _boolean = f ; }
    public BooleanValue( String str ){
        _boolean = Boolean.parseBoolean(str);;
    }
    public BooleanValue clone(){ return new BooleanValue(_boolean) ; }
    public String getValueType(){ return "B" ; }
    public boolean getBoolean(){ return _boolean ; }
    public void setBoolean( boolean f ){ _boolean = f ; }
    public void setValue( boolean f ){ _boolean = f ; }
//    public String toString(){ return super.toString()+"="+_boolean ; }
    public String getValueAsString(){ return ""+_boolean ; }
    public boolean getValueAsBoolean(){ return _boolean ; }
    public int compareTo( RosiValue value ){
    
       if( value instanceof BooleanValue ){
           boolean fvalue = ((BooleanValue)value)._boolean ;
	   return fvalue == this._boolean ? 0 : -1 ;
       }else{
          return super.compareTo(value);
       }
    }



 }
