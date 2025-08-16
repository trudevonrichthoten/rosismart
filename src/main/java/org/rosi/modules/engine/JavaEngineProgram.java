package org.rosi.modules.engine;

import org.rosi.modules.ModuleContext ;

public class JavaEngineProgram extends SimpleJavaEngine {

    class HeaterDevice {

            @DeviceAnnotation("sensor(trigger)")
            float temperature = (float) 0.0 ;

            @DeviceAnnotation("actor()")
            float desired_temperature = (float) 0.0 ;
    }
    
    class Room {

        HeaterDevice heater = new HeaterDevice() ;
        HeaterDevice mover  = new HeaterDevice() ;
    }
    
    class House {

         Room livingroom = new Room() ;
         Room hallway    = new Room() ;
    }
    House devices = new House();


    public JavaEngineProgram( ModuleContext context ) {
        super( context ) ;
        setDevices( devices );
    }

    public void runProgram(){
        System.out.println("-- Program -- : Running");

        devices.livingroom.heater.desired_temperature = (float)100.0 ;
    }

}
