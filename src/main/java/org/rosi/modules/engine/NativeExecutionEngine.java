package org.rosi.modules.engine;

import org.rosi.modules.ModuleContext ;
import org.rosi.modules.engine.nodes.BooleanValue;
import org.rosi.modules.engine.nodes.RosiActorDevice;
import org.rosi.modules.engine.nodes.RosiDevice;
import org.rosi.modules.engine.nodes.RosiMonoflopDevice;
import org.rosi.modules.engine.nodes.RosiSensorDevice;
import org.rosi.modules.engine.nodes.StringValue;
import org.rosi.modules.engine.nodes.VariableValue;
import org.rosi.modules.engine.nodes.RosiConstant;

import java.util.List;

import org.rosi.util.RosiRuntimeException;
import org.rosi.util.RosiLogable ;
import org.rosi.util.RosiLogAdapter ;

public class NativeExecutionEngine implements ProgramExecutable {
    private ModuleContext   _context = null ;
    private ProgramRegister _register = null ;
    private RosiLogable     _log = null ;

    public NativeExecutionEngine( ModuleContext context ) 
    throws RosiRuntimeException{
        _context  = context ;
        _register = new ProgramRegister() ;
        _log      = new RosiLogAdapter( "Native" , _context.getLogable() );

        _log.log( "Started" ) ;

        /*
        * The main program module.
        * .......................
        */
       String programFileName = _context.get("program");
       if( programFileName == null )
          throw new
          RosiRuntimeException("Error : 'engine.program' key not set in context","Native Engine");
    }
    private RosiSensorDevice createSensor( String sensorName , boolean shouldTrigger , RosiConstant initialValue ){

        RosiSensorDevice sensor = new RosiSensorDevice( new VariableValue( sensorName ) , initialValue ) ;
        if( shouldTrigger){
            sensor.setTrigger( shouldTrigger );
            sensor.addTriggerFunction("main");
        }
        _register.put( sensorName , sensor ) ;
        return sensor ;  
    }
    private RosiMonoflopDevice createMonoflop( String flopName , RosiSensorDevice sensor , boolean triggerOnlyIfChanged , int delay )
            throws RosiRuntimeException {

        RosiMonoflopDevice  monoflop = new RosiMonoflopDevice( flopName , delay ) ;

        monoflop.setTriggerIfChanged(triggerOnlyIfChanged);

        _register.put( flopName , monoflop ) ;

        sensor.addObserver( monoflop ) ;

        return monoflop ;
    }
    private RosiActorDevice createActor( String actorName , RosiConstant initialValue ){

        RosiActorDevice actor = new RosiActorDevice( actorName  , initialValue) ;
        _register.put( actorName , actor ) ; 
        return actor ;      
    }
    public ProgramRegister prepare() throws RosiRuntimeException{

        _log.log("Preparing Execution");

        // SENSORS
        RosiSensorDevice sensor = createSensor( 
            "bathroom.motion.counter" , 
            true , 
            new StringValue( "nix" ) ) ;

        RosiMonoflopDevice monoflop = createMonoflop( 
            "bathroom.motion.moving" , 
            sensor , 
            true , 
            120 );

       // ACTORS

        RosiActorDevice actor = createActor( 
            "bathroom.light.01.state"  , 
            new BooleanValue( false) ) ;

        return _register ;
    }
    public void initialize()  throws Exception {
        _log.log("Initiating Execution");

    }
    public void execute( List<String> functionList ) throws RosiRuntimeException{
        _log.log("Executing native program");

        RosiDevice sensor = _register.getDeviceByName("bathroom.motion.moving");

        RosiDevice actor  = _register.getDeviceByName("bathroom.light.01.state") ;
        
        if( ( sensor != null ) && ( actor != null ) && ( actor instanceof RosiActorDevice ))
             ((RosiActorDevice)actor).setValue(new BooleanValue(sensor.getValueAsBoolean())) ;  

    
    }
}