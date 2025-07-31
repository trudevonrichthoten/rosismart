package org.rosi.modules.engine.nodes ;


public class RosiActorDevice extends RosiDataDevice {

    private String _previousValue = null ; /* Don't use RosiValue here */
    
    public RosiActorDevice( String deviceName , RosiValue value ){
        super( "actor" , deviceName ,  value ) ;
    }

    public String getRosiType(){ return "A" ; }

    public void setValue( RosiValue value ){
        RosiValue current = super.getValue() ;
        if( ! value.getValueType().equals( current.getValueType() ) )
          throw new
          IllegalArgumentException(
               "Actor : "+getDeviceName()+
               " : Can't assign "+value.getValueType()+ 
               " to "+current.getValueType() );
        super.setValue(value);
    }
    public void clear( ){ 
       _previousValue = super.getValue().getValueAsString() ;
    }
    public boolean wasChanged(){ 

        if( _previousValue == null )return true ;
	    return ! super.getValue().getValueAsString().equals( _previousValue ) ;
    }
    public String formatString( String gap ){

	    StringBuffer sb = new StringBuffer() ;

	    sb.append( super.formatString(gap) ) ;
        sb.append( gap ).append("  O: ").
                         append( _previousValue == null ? "new" : ""+_previousValue.toString() ).
                         append("\n");
/*
	if( _commandString != null ){
           sb.append( gap ).append("  C: ").append( _commandString ).append("\n");
	}
*/
	return sb.toString() ;
   }
    public String toString(){
       return super.toString() + 
              "[ previous="+ ( _previousValue==null ? "new." :_previousValue.toString() )+"]" ;
    }
/*
 * DEPRICATED
    public void setCommandString( String commandString ){
       _commandString = commandString ;
    }
    public String getCommandString(){
       if( _commandString == null ){
           return "set "+getDeviceName()+ " "+getValue().getValueAsString() ;
       }else{
           return _commandString.replace( "$$" , getValue().getValueAsString() ) ;
       }
    }
 */
}
