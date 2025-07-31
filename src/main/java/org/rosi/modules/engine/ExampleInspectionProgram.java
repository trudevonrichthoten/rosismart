package org.rosi.modules.engine;

 public class ExampleInspectionProgram {

    class MotionDetector {

        String cover = "closed" ;
        int brightness = 0 ;
        String battery = "low" ;

        @DeviceAnnotation("trigger")
        String motionCount = "nothing" ;

    }
    class DoorDetector {
        String activity = "alive" ;
    
        @DeviceAnnotation("trigger")
        String trigger_cnt = "0" ;  
    }
    class HeaterDevice {

        @DeviceAnnotation("actor")
        float temperature = (float) 0.0 ;
    }
    class Room {

        DoorDetector door = new DoorDetector();
        MotionDetector motion = new MotionDetector() ;
        HeaterDevice heater = new HeaterDevice() ;
    }
    class globals {
        Room livingroom = new Room() ;
        Room hallway    = new Room();
    }
    globals top = new globals();
}
