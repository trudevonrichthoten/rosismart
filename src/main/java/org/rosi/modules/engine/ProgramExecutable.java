package org.rosi.modules.engine ;

import java.util.List ;

import org.rosi.util.RosiRuntimeException ;

public interface ProgramExecutable {

    public ProgramRegister prepare() throws RosiRuntimeException ;
    public void initialize()  throws Exception ;
    public void execute( List<String> functionList ) throws RosiRuntimeException ;

}
