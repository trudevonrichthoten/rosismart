package org.rosi.modules ;

import java.io.PrintStream;
import java.util.Map ;
import org.rosi.util.RosiLogable ;

public class ModuleContext {

   private Map<String,String> _propertyMap = null ;
   private String _name = null ;
   private RosiLogable _logable = null ;
   private PrintStream _output = null ,  _error = null ;
   public ModuleContext( String name ,  Map<String,String> propertyMap,
                         PrintStream output , PrintStream error  ){
      this( name , propertyMap );
      _output = output ;
      _error  = error ;
   }
   public ModuleContext( String name ,  Map<String,String> propertyMap ){
       _name        = name ;
       _propertyMap = propertyMap ;
   }
   public String getName(){
      return _name ;
   }
   public Map<String,String> getProperties(){
      return _propertyMap ;
   }
   public String get( String name ){
      return _propertyMap.get(name);
   }
   public String get( String name , boolean insist ) 
     throws IllegalArgumentException
   {
      String value =  _propertyMap.get(name);
      if( insist && ( value == null ) )
         throw new
         IllegalArgumentException(
            "Module "+_name+" needs '"+name+"' to be defined" ) ;
      return value ;
   }
   public void setLogable( RosiLogable logable ){
      _logable = logable ;
   }
   public RosiLogable getLogable(){
      return _logable ;
   }
}
