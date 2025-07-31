package org.rosi.modules.engine.nodes ;

public class RosiDevice extends RosiConstant {

    private String        _deviceType     = null ;
    private String        _deviceName     = null ;
    private VariableValue _deviceVariable = null ;


    public RosiDevice( String deviceType , String deviceName ){
       _deviceType = deviceType ;
       _deviceName = deviceName ;
       _deviceVariable = new VariableValue(deviceName) ;
    }

    public RosiDevice( String deviceType , VariableValue deviceValue ){
       _deviceType     = deviceType ;
       _deviceVariable = deviceValue ;
       _deviceName     = _deviceVariable.getVariableName() ;
    }
    public String getValueType(){ return "D" ; }
    public String getDeviceType(){ return _deviceType ; }
    public String getDeviceName(){ return _deviceName ; }

    public VariableValue getDeviceVariable(){ return _deviceVariable ; }

    public String toString(){
       return "Device Type = "+_deviceType+" Name = "+_deviceName ;
    }
    public String formatString( String gap ){

	StringBuffer sb = new StringBuffer() ;

	sb.append( gap ).append( _deviceType ).append("\n") ;
           sb.append( _deviceVariable.formatString( gap + "  N : " ) ) ;

	return sb.toString() ;
   }
}
