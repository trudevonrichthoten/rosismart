package org.rosi.modules.engine;

import org.rosi.modules.ModuleContext;
import org.rosi.modules.RosiModule ;
import org.rosi.modules.RosiCommandProcessor ;

import org.rosi.util.RosiRuntimeException;
import org.rosi.util.RosiSetterCommand;
import org.rosi.util.RosiTimerCommand;
import org.rosi.util.RosiLogable ;
import org.rosi.util.RosiCommand;
import org.rosi.util.RosiLogAdapter ;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleEngineModule extends RosiModule {

    private ModuleContext   _context = null ;
    private RosiLogable     _log = null ;
    private BasicEnginable  _engine =  null ;

    public SimpleEngineModule( String moduleName , ModuleContext context ) 
    throws Exception{

        super( moduleName , context );

        _context  = context ;

        log( "Started" ) ;

        String engineClassName = _context.get("program");
        log("Loading engine class : "+engineClassName);
        if( engineClassName == null )
            throw new
            RosiRuntimeException("Error : 'engine.program' key not set in context of "+this.getClass().getName());
        
        try{
            _engine = _loadEngine( engineClassName , _context );
        }catch( Exception ee ){
            errorLog("Loading Engine failed : "+ee.getMessage());
        }
    }
    public void run(){

        log("Starting with debug : "+isDebugMode());

        try{

            log("Suspending start by 4 seconds.") ;

            Thread.sleep(4000);

            log("Starting now.") ;

            _engine.initialize();

            log("'initialize' finished");

        }catch(Exception ieee ){

            errorLog( ieee.getMessage() ) ;
            errorLog( "Aborted");
            return ;
        }
        try{

            while(true){

                try{

                    RosiCommand command  = take() ;

                    boolean runExecution = false ;

                    if( command instanceof RosiTimerCommand ){

                        debug("Timer  '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;                            

                        runExecution = true ;

                    }else if( command instanceof RosiSetterCommand ){

                        debug("Setter '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

                        RosiSetterCommand setter = (RosiSetterCommand)command ;

                        runExecution = _engine.setValue( setter.getKey() , setter.getValue() ) ;

                    }else{

                        errorLog("Unkown '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;
                
                    } 
                    if( runExecution ){

                         _engine.execute();

                        Map<String,String> actors = _engine.getModifiedActors() ;
                        for( Map.Entry<String,String> set : actors.entrySet() ){
                                RosiCommand reply = new RosiSetterCommand( set.getKey() , set.getValue() ) ;
                                put( reply ) ;
                                debug("to BUS : "+reply);

                        }
                    }
                }catch(IllegalArgumentException unkownSensor ){
                    errorLog("IllegalArgumentException: "+unkownSensor.getMessage());                
                }catch(RosiRuntimeException rre ){
                    errorLog("RosiRuntimeExecption in Execution Loop (continuing) : "+rre.getMessage());
                }catch(Exception eee ){
                    if( eee instanceof InterruptedException ){
                        errorLog("Execution Loop was interrupted : "+eee.getMessage());
                        throw (InterruptedException)eee ;
                    }
                    errorLog("Exception in Execution Loop (continuing) : "+eee.getMessage() ) ;
                }
            }
        }catch(InterruptedException ieee ){
            errorLog("Interrupted: Main Loop : "+ieee.getMessage() ) ;
        }
    }
    public void setCommandProcessor( RosiCommandProcessor processor ){

    }
    private BasicEnginable _loadEngine( String className , ModuleContext context  )
            throws Exception
    {
        String commonErrorMessage = "Error initiating engine of class ("+className+") : " ;

        Class<? extends BasicEnginable> s = null ;

        try{
            s = Class.forName( className ).
                asSubclass( org.rosi.modules.engine.BasicEnginable.class ) ;
        }catch(Exception cnfe ){
            cnfe.printStackTrace();

            throw new
            IllegalArgumentException( "Error finding engine class : class not found : "+className);
        }
        Constructor<? extends BasicEnginable> x = null ; 
        try{
            x = s.getConstructor( 
                  org.rosi.modules.ModuleContext.class
                              );
        }catch(NoSuchMethodException nsme ){
            throw new
            IllegalArgumentException( commonErrorMessage+nsme.getMessage());
        }
        try{

            return x.newInstance( context  ) ;
      
        }catch(InvocationTargetException ie ){
            Throwable t = ie.getTargetException() ;
            if( t instanceof RosiRuntimeException )
                throw ((RosiRuntimeException)t);

            throw new
            IllegalArgumentException( 
                commonErrorMessage+" : ("+t.getClass().getName()+") "+t.getMessage() 
                                    );
        }catch(Exception ee ){
            throw new
            IllegalArgumentException( 
                commonErrorMessage+" : ("+ee.getClass().getName()+") "+ee.getMessage() 
                                    );
        }
    }
}
