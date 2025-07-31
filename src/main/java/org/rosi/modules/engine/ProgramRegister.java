package org.rosi.modules.engine ;

import org.rosi.modules.engine.nodes.* ;
import org.rosi.util.*;

import java.util.* ;
import java.util.Map.Entry;


public class ProgramRegister {

   private Map<String,RosiValue>  _globals = new HashMap<String,RosiValue>() ;
  
   public ProgramRegister(){}
   public boolean checkIfGlobal( String name ){
      return _globals.containsKey(name);
   }
   public Map<String,RosiValue> getGlobals(){ return _globals ; }

   public Set<Map.Entry<String,RosiValue>> entrySet(){ 
/* -----------------------------------------*/
       return _globals.entrySet() ; 
   }
   public Map<String,RosiValue> sorted(){  
/* ---------------------------------------*/
       return new TreeMap<String,RosiValue>(_globals) ; 
   }
   public void put( String name , RosiValue value ){
      _globals.put( name , value ) ;
   }
   public RosiDevice getDeviceByName( String name )throws IllegalArgumentException, RosiRuntimeException {
      RosiValue device = _globals.get(name) ;

      if( device == null )
          throw new
          IllegalArgumentException( "Device not found : "+name ) ; 

      if( ! ( device instanceof RosiDevice ) )
         throw new
         RosiRuntimeException("FATAL: Name found but not a device: "+device.getClass().getName());

      return (RosiDevice) device ;
   }
/**
  *   Managing the Sensors
  *   --------------------
  *
  */
   public RosiValue getSensorValue( String sensorName ) throws IllegalArgumentException , RosiNotFoundException {
/* ---------------------------------------------------------------------------------------*/
  
       RosiValue value = _globals.get( sensorName ) ;
       if( value == null )
          throw new
          RosiNotFoundException( "Sensor not found : "+sensorName ) ;

       if( ! (value instanceof RosiSensorDevice ) )
           throw new
           IllegalArgumentException( "Not a sensor : "+sensorName ) ;

       return ((RosiSensorDevice)value).getValue() ;
   }
   /**
     *
     */
   public List<String> setSensorValue( String sensorName , String sensorValue ) throws RosiRuntimeException {
/* --------------------------------------------------------------------------------------------------------*/

       RosiSensorDevice device = getSensor( sensorName );

       device.setSensorValue( sensorValue ) ;

       return ((RosiSensorDevice)device).functions() ;

   }

   public void setSensor( String sensorName , String sensorValue ) throws IllegalArgumentException , RosiNotFoundException {
/* -------------------------------------------------------------------------------------------------*/

       RosiValue value = getSensorValue( sensorName ) ;

       if( ! ( value instanceof StringValue ) )
          throw new
          IllegalArgumentException("Not a 'string' sensor : "+sensorValue ) ;

       ((StringValue)value).setValue( sensorValue ) ;

   }
   public void setSensor( String sensorName , float sensorValue ) throws IllegalArgumentException, RosiNotFoundException {
/* -----------------------------------------*/

       RosiValue value = getSensorValue( sensorName ) ;

       if( value instanceof FloatValue ){

           ((FloatValue)value).setValue( sensorValue ) ;

       }else{

          throw new
          IllegalArgumentException("Not a 'float' sensor : "+sensorValue ) ;

       }
   }
   public void setSensor( String sensorName , int sensorValue ) throws IllegalArgumentException, RosiNotFoundException {
/* -----------------------------------------*/

       RosiValue value = getSensorValue( sensorName ) ;

       if( value instanceof NumberValue ){

           ((NumberValue)value).setValue( sensorValue ) ;

       }else if( value instanceof FloatValue ){

           ((FloatValue)value).setValue( (float)sensorValue ) ;

       }else{

          throw new
          IllegalArgumentException("Not a 'float' sensor : "+sensorValue ) ;

       }
   }
   /**
     *
     */
   private RosiSensorDevice getSensor( String sensorName ) throws IllegalArgumentException {
/* -----------------------------------------------------------------------------------------*/

       RosiValue value = _globals.get( sensorName ) ;
       if( value == null )
          throw new
          IllegalArgumentException( "Sensor not found : "+sensorName ) ;

       if( ! (value instanceof RosiSensorDevice ) )
           throw new
           IllegalArgumentException( "Not a sensor : "+sensorName ) ;

       return (RosiSensorDevice)value ;

   }
   public boolean shouldTrigger( String sensorName ) throws IllegalArgumentException {
/* -----------------------------------------------------------------------------------*/

       return ((RosiSensorDevice)getSensor(sensorName)).isTrigger() ;

   }
/**
  *   Managing the Actors
  *   -------------------
  *
  */
   public void clearActors(){
/* --------------------------*/

      for( Map.Entry<String,RosiValue> entry : _globals.entrySet() ){

          RosiValue variable  = entry.getValue() ;

          if( variable instanceof RosiActorDevice ){
             ((RosiActorDevice)variable).clear() ;
          }
      }

   }

   private List<RosiActorDevice> getActors(){
/* -----------------------------------------*/

      List<RosiActorDevice> list = new ArrayList<RosiActorDevice>() ;

      for( Map.Entry<String,RosiValue> entry : _globals.entrySet() ){

         RosiValue value = entry.getValue() ;

         if(  value instanceof RosiActorDevice   ){

            list.add( (RosiActorDevice)value ) ;
         }
      }
      return list ;

   }
   private List<RosiActorDevice> getChangedActors(){
/* -------------------------------------------------*/
      List<RosiActorDevice> list = new ArrayList<RosiActorDevice>() ;
      for( Map.Entry<String,RosiValue> entry : _globals.entrySet() ){

         RosiValue value = entry.getValue() ;

         if( (  value instanceof RosiActorDevice   )   &&
            ( ((RosiActorDevice)value).wasChanged() )    ){

            list.add( (RosiActorDevice)value ) ;
         }
      }
      return list ;

   }
   public Map<String,String> getChangedActorValues(){
/* -------------------------------------------------*/
      Map<String,String> map = new HashMap<String,String>() ;
      for( Map.Entry<String,RosiValue> entry : _globals.entrySet() ){

         RosiValue value = entry.getValue() ;

         if( value instanceof RosiActorDevice ){

            RosiActorDevice device = (RosiActorDevice)value ;
            if( device.wasChanged() )
               map.put( device.getDeviceName() , device.getValueAsString() ) ;
         }
      }
      return map ;
   }

   public void clearMonoflops(){
/* --------------------------------*/

      for( Map.Entry<String,RosiValue> entry : _globals.entrySet() ){

         RosiValue variable  = entry.getValue() ;

         if( variable instanceof RosiMonoflopDevice ){
            ((RosiMonoflopDevice)variable).clear() ;
         }
      }
   }
/**
  *   Printouts 
  *   -------------------
  *
  */
   public String dumpActors(){
/* -----------------------------------------*/
     StringBuffer sb = new StringBuffer();

      List<RosiActorDevice> actorsList = getActors() ;

      sb.append("-- Current Status of Actors --\n");

         for( RosiActorDevice actor : actorsList ){
            sb.append("  "+actor.getDeviceName()+" : "+actor.getValue()+" "+actor.wasChanged()+"\n");
         }
      sb.append("------------------------------\n");
      return sb.toString() ;
   }
   public String dumpRegisters(){
/* --------------------------------*/

      int maxLen = 0 ;
      for( Map.Entry<String,RosiValue> entry : _globals.entrySet() ){

         String variableName = entry.getKey().toString();

         if( entry.getValue() instanceof RosiArray ){
            RosiArray array = (RosiArray)entry.getValue() ;
            Map<String,RosiConstant> map = array.getMap();          
            for( Map.Entry<String,RosiConstant> c : map.entrySet() ){
               String indexName = c.getKey().toString() ;
               maxLen = Math.max( (variableName.length()+indexName.length()+2) , maxLen ) ;
            }
         }else{
            maxLen = Math.max( variableName.length() , maxLen ) ;
         }
      }
      maxLen += 2 ;

      Map<String,RosiValue> sorted = this.sorted() ;

      StringBuffer sb = new StringBuffer() ;
      for( Map.Entry<String,RosiValue> entry : sorted.entrySet() ){
          
          if( entry.getValue() instanceof RosiArray ){

             RosiArray array = (RosiArray)entry.getValue() ;

             Map<String,RosiConstant> map = array.getMap();

             for( Map.Entry<String,RosiConstant> c : map.entrySet() ){

               String variableName = entry.getKey().toString() ;
               String indexName    = c.getKey().toString() ;

               sb.append(variableName).append("[").append(indexName).append("]") ; 

               for( int i = ( variableName.length() + indexName.length() ) ; i < maxLen ; i++ )
                  sb.append(".") ;
                  
               sb.append( " [").append( c.getValue().getRosiType() ).
                                 append( c.getValue().getValueType() ).
                                 append("] .. ");

               sb.append( c.getValue().getValueAsString() ) ;
               sb.append("\n");
            }
          }else{

             String variableName = entry.getKey().toString() ;
             sb.append(variableName).append(" ") ;
             for( int i = variableName.length() ; i < maxLen ; i++ )
                sb.append(".") ;
             sb.append(" ");
             sb.append( " [").append( entry.getValue().getRosiType() ).
                              append( entry.getValue().getValueType() ).
                append("] .. ");
             sb.append( entry.getValue().getValueAsString() ) ;
/*
             sb.append( " ... ").append( entry.getValue().getClass().getName() ) ;
             if( entry.getValue() instanceof RosiDataDevice )
             sb.append( " ... ").append( ((RosiDataDevice)entry.getValue()).getValue().getClass().getName() ) ;
*/
             sb.append("\n");
          }
      }
      return sb.toString();
  }
  public static void main( String [] args ) throws Exception {
      Stack<String> s = new Stack<String>() ;
      s.push("fist") ;
      s.push("second") ;
      for( String x : s ){
         System.out.println("found : "+x );
      }

      System.out.println("x : "+s.firstElement());

  }
}

