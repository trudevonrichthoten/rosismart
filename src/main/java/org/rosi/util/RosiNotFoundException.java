package org.rosi.util;

public class RosiNotFoundException extends RosiRuntimeException {
    public RosiNotFoundException( String message ){
        super(message);
    }
    public RosiNotFoundException( String message , String context ){
        super(message , context);
    }
}
