package org.rosi.modules.engine.nodes ;


public class RosiMonoflopDevice extends RosiDeviceObserver {

    private long    _delay        = 0 ;
    private long    _timer        = 0L ;
    private boolean _forceTrigger = false ;
    
    public RosiMonoflopDevice( String deviceName , String targetName , long delay ){
        super( "monoflop" , deviceName , targetName ) ;
	    _delay  = delay ;
    }
    public RosiMonoflopDevice( String deviceName , long delay ){
        super( "monoflop" , deviceName , "target" ) ;
	    _delay  = delay ;
    }
    public String getValueType(){ return "B" ; }
    public String getRosiType(){ return "M" ; }

    public void trigger(){
    
        _timer = System.currentTimeMillis() ;
        if( _delay == 0 )_forceTrigger = true ;
   
    }
    public void clear(){
        _forceTrigger = false ;
    }
    public String getValueAsString(){ return ""+getValueAsBoolean() ; }
    public boolean getValueAsBoolean(){
        if( _delay == 0 ){
           return _forceTrigger ;
        }else{
           return ( System.currentTimeMillis() - _timer ) < ( _delay * 1000 ) ;
        }
    }
    private long remainingTime(){

        if( _delay == 0 )return 0L ;

        long del = System.currentTimeMillis() - _timer  ;
	    return del > ( _delay * 1000L ) ?
	       0L :
	       ( System.currentTimeMillis() - _timer ) / 1000L ;
	       
    }
    public String formatString( String gap ){

	    StringBuffer sb = new StringBuffer() ;

	    sb.append( super.formatString(gap) ) ;
        sb.append( gap ).append("  D: ").append(_delay).append(" seconds\n") ;
	    sb.append( gap ).append("  S: ").append(getValueAsBoolean()).append(" (").append(remainingTime()).append(")\n");

	    return sb.toString() ;
    }
   public String toString(){
       return super.toString() +" [state "+getValueAsBoolean()+"] {"+_delay+"/"+remainingTime()+"}" ;
    }
}
