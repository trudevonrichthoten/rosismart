package org.rosi.util ;

import java.time.Instant ;
import java.util.Map ;
import java.util.HashMap;

public class MemoryKeyValueManager {

    private class Entry {
       private String _key   = null ;
       private String _value = null ;
       private Instant _time = Instant.now();
       private Entry( String key , String value ){
          this._key   = key ;
          this._value = value ;
       }
       private Instant getCreationTime(){
          return _time ;
       }
       private String getKey(){ return _key ; }
       private String getValue(){ return _value ; }
    }
    private Map<String,Entry> _map = new HashMap<String,Entry>();
    public MemoryKeyValueManager(){

    }

    public synchronized void add( String key , String value ){
        _map.put( key , new Entry(key,value) ) ;
    }
    public synchronized String get( String key ){
       Entry e = _map.get(key) ;
       if( e == null )return null ;
       return e.getValue() ;
    }
    public synchronized Map<String,String> getSimpleMap(){
       Map<String,String> map = new HashMap<String,String>() ;
       for( Entry e : _map.values() ){
          map.put( e.getKey() , e.getValue() ) ;
       }
       return map;
    }
}
