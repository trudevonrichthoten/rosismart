package org.rosi.modules.engine.nodes ;

public class FloatValue extends RosiConstant {
 
    private float _float = (float)0.0 ;
    public FloatValue( String str ){
        _float = Float.parseFloat( str ) ;
    }
    public FloatValue( float f ){  _float = f ; }
    public FloatValue( double f ){  _float = (float)f ; }
    public String getValueType(){ return "F" ; }
    public float getFloat(){ return _float ; }
    public void setFloat( float f ){ _float = f ; }
    public void setValue( float f ){ _float = f ; }
//    public String toString(){ return super.toString()+"="+_float ; }

    public FloatValue clone(){ return new FloatValue(_float) ; }

    public String getValueAsString(){ return ""+_float; }
    
    public float getValueAsFloat(){return _float ;}

//    public boolean getValueAsBoolean(){ return _float != 0 ; }
    
    public int compareTo( RosiValue value ){
    
       float fvalue = (float)0.0 ;
       if( value instanceof FloatValue ){
       
           fvalue = ((FloatValue)value)._float ;
	   
       }else if( value instanceof NumberValue ){
       
           fvalue = ((NumberValue)value).getValueAsFloat() ;
	   
       }else{
          return super.compareTo(value);
       }
       return fvalue == this._float ? 0 : ( fvalue > this._float ? -1 : 1 ) ;
    }
 }
