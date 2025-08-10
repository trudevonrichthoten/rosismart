package org.rosi.modules.engine;

import org.rosi.modules.ModuleContext ;

public class JavaEngineProgram extends SimpleJavaEngine {

    class HeaterDevice {

            @DeviceAnnotation("sensor()")
            float temperature = (float) 0.0 ;

            @DeviceAnnotation("actor()")
            float desired_temperature = (float) 0.0 ;
    }
    
    class x {

        HeaterDevice heater = new HeaterDevice() ;
    }

    x devices = new x();


    public JavaEngineProgram( ModuleContext context ) {
        super( context ) ;
        setDevices( devices );
    }

    public void runProgram(){
        devices.heater.desired_temperature = (float)100.0 ;
        System.out.println("Running program");
    }

}
