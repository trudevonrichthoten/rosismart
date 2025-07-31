package org.rosi.modules.engine.nodes ;

import java.util.List ;
import java.util.ArrayList ;

import org.rosi.util.*;

public class RosiSensorDevice extends RosiDataDevice {

    private List<RosiDeviceObserver>  _observer = new ArrayList<RosiDeviceObserver>() ;
    private boolean _isTrigger          = false ;
    private boolean _valueHasChanged    = false ;
    private boolean _isTriggerIfChanged = false ;
    private List<String> _functions     = new ArrayList<String>();

    public RosiSensorDevice( VariableValue deviceVariable , RosiValue value ){
    
        super( "sensor" , deviceVariable , value ) ;

    }
    public String getRosiType(){ return "S" ; }

    public void addObserver( RosiDeviceObserver observer ){
    
        observer.setValue( getValue() , false ) ;
        _observer.add( observer ) ;
	
    }
    public void addTriggerFunction( String functionName ){
       _functions.add(functionName);
    }
    public void setTrigger( boolean trigger ){
        _isTrigger = trigger ; 
    }
    public boolean isTrigger(){ 
        
	//return _isTrigger || ( _functions.size() > 0 ) ; 

	return ( _isTrigger ) || 
               ( _isTriggerIfChanged && _valueHasChanged ) ; 
	
    }
    public List<String> functions(){ 
      return _functions ;
    }
    public void setTriggerIfChanged(){
        _isTriggerIfChanged = true ; 
    }
    public void setTriggerIfChanged( boolean trigger ){
        _isTriggerIfChanged = trigger ; 
    }
    public boolean isTriggerIfChanged(){ 
        
	return _isTriggerIfChanged ; 
	
    }
    public void setSensorValue(  String sensorValue ) throws RosiRuntimeException {
   
       RosiValue value = getValue() ;
  
       RosiValue oldValue = value.clone();
      
       try{ 
         if(  value instanceof StringValue ){
       
            ((StringValue)value).setValue( sensorValue ) ;
	    
         }else if( value instanceof FloatValue ){
       
            ((FloatValue)value).setValue( Float.parseFloat( sensorValue ) ) ;
	    
         }else if( value instanceof NumberValue ){
       
            ((NumberValue)value).setValue( Integer.parseInt( sensorValue ) ) ;
	    
         }else{
       
            throw new
	    IllegalArgumentException("Can't convert >"+sensorValue+"< to "+value.getClass().getName() ) ;
	  
       
         }      
       }catch(Exception ne ){
          throw new
          RosiRuntimeException("Value type mismatch : Can't convert "+sensorValue+" to "+value.getClass().getName() ) ;
       }

       _valueHasChanged = oldValue.compareTo( value ) != 0 ;

       for( RosiDeviceObserver observer : _observer ) observer.setValue(value);
       
    }
    public String formatString( String gap ){

        StringBuffer sb = new StringBuffer() ;

        sb.append( super.formatString(gap) ) ;
        if( _functions.size() > 0 ){
          sb.append( gap ).append( "  F: ") ;
          for( String s : _functions )sb.append(s).append(",");
          sb.append("\n");
        }

        if( _observer.size() > 0 ){
           sb.append( gap ).append( "  O: ") ;
           for( RosiDeviceObserver observer : _observer )
	      sb.append(observer.getDeviceName() ).append(",") ;
           sb.append("\n");
        } 

        return sb.toString() ;
   }
    public String toString(){
    
       StringBuffer sb = new StringBuffer() ;
       
       sb.append( super.toString() ).append(" {") ;
	for( RosiDeviceObserver observer : _observer ){
	    sb.append(observer.getDeviceName() ).append(",") ;
       }
       sb.append("}").append( _isTrigger ? "{trigger}" : "{no-trigger}" ) ;
       
       return sb.toString() ;
    }
}
