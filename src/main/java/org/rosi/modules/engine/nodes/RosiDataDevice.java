package org.rosi.modules.engine.nodes ;


public class RosiDataDevice extends RosiDevice {

    private RosiValue _value = null ;
    
    public RosiDataDevice( String deviceType , String deviceName , RosiValue value ){
    
        super( deviceType , deviceName ) ;
        _value = value ;
    }
    public RosiDataDevice( String deviceType , VariableValue deviceVariable , RosiValue value ){
    
        super( deviceType , deviceVariable ) ;
        _value = value ;
	
    }
    public RosiValue getValue(){ 
       return _value ; 
    }
    public String getValueType(){ return _value.getValueType() ; }
    public String getRosiType(){ return "D" ; }

    public void setValue( RosiValue value ){
       _value = value ;
    }
    public String getValueAsString(){ 
       return _value.getValueAsString() ; 
    }
    public String formatString( String gap ){

	StringBuffer sb = new StringBuffer() ;

	sb.append( super.formatString(gap) ) ;
        sb.append( _value.formatString( gap + "  V : ")  ) ;

	return sb.toString() ;
   }
   public String toString(){
   
       return getDeviceType()+" "+getDeviceName()+" ["+_value+"]" ;
       
   }
}
