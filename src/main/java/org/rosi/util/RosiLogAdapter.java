package org.rosi.util;

public class RosiLogAdapter implements RosiLogable {
    private String      _tag ;
    private RosiLogable _out ;
    public RosiLogAdapter( String tag  , RosiLogable out ){
        _out = out ;
        _tag = tag ;
    }
    public void log( String message ){
        _out.log( " ["+_tag+"] "+message ) ;
    }
    public void errorLog( String message ){
        _out.errorLog( " ["+_tag+"] "+message ) ;
    }
    public void debug( String message ){
        _out.debug( " ["+_tag+"] "+message ) ;
    }
}
