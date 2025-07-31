 package org.rosi.modules.engine.nodes ;

 
 public class NumberValue extends RosiConstant {
 
    private long _int = 0 ;
    public NumberValue( String str ){
        _int = Integer.parseInt( str ) ;
    }
    public NumberValue( long f ){  _int = f ; }
    public String getValueType(){ return "N" ; }
    public long getInt(){ return _int ; }
    public void setInt( long f ){ _int = f ; }
    public void setValue( long f ){ _int = f ; }

    public NumberValue clone(){
       return new NumberValue(_int);
    }

    public String getValueAsString(){ return ""+_int ; }
    
    public boolean getValueAsBoolean(){ return _int != 0 ; }
    
    public float getValueAsFloat(){ return (float)_int ; }

    public long getValueAsNumber(){ return _int ; }
    
    public int compareTo( RosiValue value ){
    
       long fvalue = 0 ;
       if( value instanceof NumberValue ){
       
           fvalue = ((NumberValue)value)._int ;
	   
       }else if( value instanceof FloatValue ){
       
           fvalue = (long) ((FloatValue)value).getValueAsFloat() ;
	   
       }else{
          return super.compareTo(value);
       }
 
       return fvalue == this._int ? 0 : ( fvalue > this._int ? -1 : 1 ) ;

    }



 }
