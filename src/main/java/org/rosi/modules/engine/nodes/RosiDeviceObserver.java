package org.rosi.modules.engine.nodes ;


public class RosiDeviceObserver extends RosiDevice {

    private String    _targetName       = null ;
    private RosiValue _triggerValue     = null ;
    private RosiValue _previousValue    = null ;
    private boolean   _triggerIfChanged = false ;

    public RosiDeviceObserver( String deviceType , String deviceName , String targetName ){

        super( deviceType , deviceName ) ;

        _targetName = targetName ;

    }
    public String getValueType(){ return "B" ; }
    public String getRosiType(){ return "O" ; }

    public String getTargetName(){ 
    
       return _targetName ; 
       
    }
    public void setTriggerIfChanged( boolean ifChanged ){
      _triggerIfChanged = ifChanged ;
    }
    public void addTriggerValue( RosiValue value ){

       if( ( _previousValue == null                        ) ||
           ( _previousValue.getClass() == value.getClass() )    ){

          _triggerValue = value ;

       }else{

            throw new
            IllegalArgumentException("addTriggerValue: different types : triggerValue != previousValue ");
       }
    }
    public void setValue( RosiValue value ){
       setValue( value , true ) ;
    }
    public void setValue( RosiValue value  , boolean touch ){
    
       if( ! touch )return ;

       if( _triggerIfChanged ){

          if( ( _previousValue == null                 ) ||
              ( _previousValue.compareTo( value ) != 0 )    )trigger() ;
          
       }else if( _triggerValue != null ){

          if( value.compareTo( _triggerValue ) == 0 )trigger() ;

       }else{

          trigger() ;

       }

       _previousValue = value.clone() ;

       return ;
      
    }
    public void trigger(){ }
    public String formatString( String gap ){

	StringBuffer sb = new StringBuffer() ;

	sb.append( super.formatString(gap) ) ;
        sb.append( gap ).append("  T: ").append(_targetName).append("\n") ;
        sb.append( gap ).append("  M: ").
           append( _triggerIfChanged ? "if_changed" :
                                       (  _triggerValue != null ? "triggeredByValue" :
                                                                  "anytime" ) ).
           append("\n");
        sb.append( gap ).
           append("  X: ").
           append(_triggerValue==null ? "*" : _triggerValue.getValueAsString() ).
           append("\n");
        sb.append( gap ).
           append("  P: ").
           append(_previousValue==null ? "*" : _previousValue.getValueAsString() ).
           append("\n");

	return sb.toString() ;
   }
   public String toString(){
       return super.toString() +" {"+_targetName+"}" ;
   }
}
