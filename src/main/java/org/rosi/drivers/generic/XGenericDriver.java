package org.rosi.drivers.generic ;

import java.util.Map ;
import java.util.List ;

public interface XGenericDriver {

    public List<String> getDeviceNames() throws Exception ;

    public void setDeviceAttribute( String deviceName , String key , String value ) throws Exception ;

    public Map<String,String> getDeviceAttributes( String deviceName )  throws Exception;

    public void update() throws Exception;

}
