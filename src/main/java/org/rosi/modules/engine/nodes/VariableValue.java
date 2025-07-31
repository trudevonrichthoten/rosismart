package org.rosi.modules.engine.nodes ;

 

 public class VariableValue extends RosiValue {
 
    private String _string = null  ;
    private RosiValue _expression = null ;    

    public VariableValue( String f ){  _string = f ; }
    public String getVariableName(){ return _string ; }
    public void setString( String f ){ _string = f ; }
    public String getValueAsString(){ return _string + ( _expression != null ? "[]" : "" ) ; }
    public String toString(){ return  super.toString()+"="+_string ; }
    public void setIndexExpression( RosiValue expr ){
       _expression= expr ;
    }
    public RosiValue getIndexExpression(){ return _expression ; }
    public boolean isArray(){ return _expression != null ; }
    public String formatString( String gap ){

        StringBuffer sb = new StringBuffer() ;

        sb.append( gap ).append( "Variable" ).append("\n") ;
        sb.append( gap ).append( "  N : ").append(_string).append("\n");
        if( _expression != null)
           sb.append( _expression.formatString( gap + "  I : " )  ) ;


        return sb.toString() ;
   }

    



 }
