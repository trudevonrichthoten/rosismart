package org.rosi.modules.engine;

import java.lang.reflect.*;
import java.util.*;

import org.rosi.util.RosiNotFoundException;

public class InspectionCompanion {
    private Map<String,FieldDetails> _deviceMap = new HashMap<String,FieldDetails>();
    public class FieldDetails {
        private Field  _field ;
        private Class  _callingClass ;
        private Object _obj ;
        private Class  _type ;
        private String _details ;
        private FieldDetails( Field field , Class callingClass , Object obj , Class fieldClass , String details ){
          _field   = field ;
          _callingClass = callingClass ;
          _obj     = obj;
          _type    = fieldClass ;
          _details = details ;
        }
        public String getAnnotations(){ return _details ; }
        public String toString(){
            try{
                return "C=["+_type.getName()+"];D=["+_details+"];O=["+_field.get(_obj).toString()+"]";
            }catch(Exception ee ){
                return "C=["+_type.getName()+"];D=["+_details+"];O=[N.A.]";
            }        
        }
        public Class getCompanionClass(){
            return _type ;
        }
        public int getInt() throws Exception {
            return _field.getInt(_obj);
        }    
        public float getFloat() throws Exception {
            return _field.getFloat(_obj);
        }
        public double getDouble() throws Exception {
            return _field.getDouble(_obj);
        }
        public boolean getBoolean() throws Exception {
            return _field.getBoolean(_obj);
        }
        public String getString() throws Exception {
            return _field.get(_obj).toString();
        }
        public void set( String value ) throws Exception {
            if( _type == java.lang.Integer.class ){
                _field.setInt( _obj , Integer.parseInt( value) ) ;
            }else if( _type == java.lang.Float.class ){
                _field.setFloat( _obj , Float.parseFloat( value) ) ;
            }else if( _type == java.lang.Double.class ){
                _field.setDouble( _obj , Double.parseDouble( value) ) ;
            }else if( _type == java.lang.Long.class ){
                _field.setBoolean( _obj , Boolean.parseBoolean( value) ) ;
            }else if( _type == java.lang.String.class ){
                _field.set( _obj , value ) ;
            }else{
                throw new Exception("Unsupported internal Class : "+_type.toString());
            }
        }
    }
    public InspectionCompanion( String name , Object in_object ) throws Exception {
        walkObject( in_object , _deviceMap , name );
    }
    public FieldDetails getByName( String name ) throws RosiNotFoundException {
        FieldDetails details = _deviceMap.get(name) ;
        if( details == null )
            throw new 
            RosiNotFoundException( "Device name not found." , name ) ;
        return details ;
    }
    public Set<String> names(){
        return _deviceMap.keySet() ;
    }
    public int getInt( String name ) throws Exception {
        return getByName(name).getInt();
    }    
    public float getFloat( String name ) throws Exception {
        return getByName(name).getFloat() ;
    }
    public double getDouble( String name ) throws Exception {
        return getByName(name).getDouble() ;
    }
    public boolean getBoolean( String name ) throws Exception {
        return getByName(name).getBoolean() ;
    }
    public String getString( String name ) throws Exception {
        return getByName(name).toString();
    }
    public String getAnnotations( String name ) throws Exception {
        return getByName(name)._details ;
    }
    public Class getClass( String name ) throws Exception {
        return getByName(name)._type ;
    }
    public void  walkObject( Object obj , Map<String,FieldDetails> map , String prefix) throws Exception {
        Class cls = obj.getClass();
        System.out.println("Walking starts at "+cls.getName());
        
        Field fieldlist[] = cls.getDeclaredFields() ; 
        for( int i = 0 ; i < fieldlist.length ; i++ ){

            Field field  = fieldlist[i];
            System.out.println("Found : "+field.getName());
            if( field.getName().equals("this$0") )continue ;
            String name  = prefix+"."+field.getName();
            Class type   = field.getType();
            boolean p    = type.isPrimitive() ;
            DeviceAnnotation dd = field.getAnnotation(DeviceAnnotation.class);
            String details = dd == null ? "" : dd.value();

            if( p  || ( type == java.lang.String.class ) ){
                    FieldDetails entry = new FieldDetails( field , cls , obj,type,details);
                    map.put( name , entry  );
            }else{
                walkObject( field.get(obj) , map , name );
            }
        }
    }
  

    public static void main (String [] args )throws Exception {
        ExampleInspectionProgram globals = new ExampleInspectionProgram() ;
        InspectionCompanion companion = new InspectionCompanion( "something" , globals );
        Set<String> stringSet = companion.names() ;
        System.out.println("Listing devices!");
        for( String name : stringSet ){
            System.out.println(name+" -> "+companion.getByName(name) ) ;
        }
    }
}
