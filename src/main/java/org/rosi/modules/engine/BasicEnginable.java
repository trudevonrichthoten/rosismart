package org.rosi.modules.engine;
import java.util.Map;

public interface BasicEnginable {
    public void initialize() throws Exception ;
    public boolean setValue( String key , String value ) throws Exception  ;
    public void execute() throws Exception ;
    public Map<String,String> getModifiedActors() throws Exception  ;
}
