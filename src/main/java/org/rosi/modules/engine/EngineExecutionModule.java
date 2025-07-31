package org.rosi.modules.engine;

import java.util.* ;
import java.io.* ;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.rosi.util.*;
import org.rosi.modules.*;

public class EngineExecutionModule extends RosiModule {
 
   private ProgramRegister      _register     = null ;
   private PrintStream          _rosiOutput   = System.out ;
   private PrintStream          _rosiError    = System.err ;     

   private long                 _suspendTime = 10000L;
   private boolean              _resetActors = false ;
   private ProgramExecutable    _executable  = null ;

   public EngineExecutionModule(
             String moduleName , 
             ModuleContext context ) throws Exception {

       super(moduleName,context);

       log("Initiating.");
       
        /*
         * I rosi input and ouput.
         * .......................
         */
       String rosiOutputString = _context.get("rosiOutput" );
       if( rosiOutputString != null ){
          File f = new File( rosiOutputString ) ;
          _rosiOutput = new PrintStream( new FileOutputStream( f , true ) ) ;
       }
       rosiOutputString = _context.get("rosiError" );
       if( rosiOutputString != null ){
          File f = new File( rosiOutputString ) ;
          _rosiError = new PrintStream( new FileOutputStream( f , true ) ) ;
       }
       /*
        * Define the window, within we don't trigger, even if the 
        * Sensor is defined 'trigger'. (currently ignored)
        * .......................
        */
       String catchTime = _context.get("catchTime") ;
       if( catchTime  != null ){
          log("New Catch Time : "+catchTime );
          try{
              _suspendTime = Integer.parseInt(catchTime) * 1000L ;
          }catch(Exception ee ){
             errorLog("Couldn't convert catchTime to 'integer'"+catchTime ) ;
          }
        }
        log("Using catch time : "+_suspendTime+" ms");
        /*
         * Initial actor reset. 
         * .......................====
         */
       String resetActorString = _context.get("resetActors");

       _resetActors = ! ( ( resetActorString == null ) || resetActorString.equals("false") ) ;

       String executionClassName = _context.get("engineClass");
       executionClassName = executionClassName == null ?  
                            "org.rosi.modules.engine.NativeExecutionEngine": 
                            executionClassName ;
                            
       _executable = _loadExecutable( executionClassName,_context  ) ;

      long baseTime = System.currentTimeMillis() ;
      log("Preparing Execution Engine;");

      _register = _executable.prepare() ;

      log( "Preparing execution engine took : "+(System.currentTimeMillis() - baseTime )+" millis" ) ;

   } 
   /*
    * ---------------------------------------------------------------------------------------------------------------------
    */
   private void initializeExecutionEngine() throws Exception{

       long baseTime = System.currentTimeMillis() ;

       if( isDebugMode() )dumpRegister();

       baseTime = System.currentTimeMillis() ;

      _executable.initialize() ;

      if( _resetActors ){
           log("Running Actors");
           runAndClearActors() ;
      } 

      log("Running initialization took  : "+(System.currentTimeMillis() - baseTime )+" millis" ) ;

      if( isDebugMode() )dumpRegister();
   }
   public void run(){

      log("Starting with debug : "+isDebugMode());

      try{

         log("Suspending start by 4 seconds.") ;

         Thread.sleep(4000);

         log("Starting now.") ;

         initializeExecutionEngine();

         log("prepareExecutionEngine finished");

      }catch(Exception ieee ){

         errorLog( ieee.getMessage() ) ;
         errorLog( "Aborted");
         return ;
      }
      try{

         int  triggerCount = 0 ;
         long timeStamp    = 0L ;
         List<String> callMain = new ArrayList<String>();
         callMain.add("main");

         while(true){

            try{

               RosiCommand command  = take() ;

               List<String> triggerCommands = processCommand( command ) ;
               if( triggerCommands.size() > 0 )triggerCount++ ;

               if( triggerCount == 1 ) {
                  timeStamp = System.currentTimeMillis() ;
                  triggerCount ++ ;
               }

               long timeDiff = System.currentTimeMillis() - timeStamp ;

               if( ( triggerCount > 0 ) && ( mightBlock() || ( timeDiff > _suspendTime ) ) ){ 
 
                  debug("Executing 'engine' after TC="+triggerCount+";MB="+mightBlock()+";t="+timeDiff);

                  execute( callMain ) ;

                  triggerCount = 0 ;

               }
            }catch(RosiRuntimeException rre ){
               errorLog("Execution Loop : "+rre.getMessage());
            }catch(Exception eee ){
               if( eee instanceof InterruptedException )throw (InterruptedException)eee ;
               errorLog("Execution Loop : "+eee.getMessage() ) ;
            }
         }
      }catch(InterruptedException ieee ){
         errorLog("Interrupted: Main Loop : "+ieee.getMessage() ) ;
      }
   }
   private List<String> processCommand( RosiCommand command ) throws Exception {
 
      List<String> array = new ArrayList<String>() ;

      if( command instanceof RosiTimerCommand ){

         debug("Timer  '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

//           RosiTimerCommand timeCommand = (RosiTimerCommand)command ;
//           _code.setTime( timeCommand.getCalendar() ) ;
         array.add("click");

      }else if( command instanceof RosiSetterCommand ){

         debug("Setter '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

         RosiSetterCommand setter = (RosiSetterCommand)command ;

         array = _register.setSensorValue( setter.getKey() , setter.getValue() ) ;

      }else{

         log("Unkown '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;
                
      }
      return array;

   }
   private void execute() throws Exception { execute(null) ; } ;
   private void execute( List<String> functionList ) throws Exception {

      synchronized( _executable ){_executable.execute( functionList ) ;}

      if( isDebugMode() )dumpRegister();

      runAndClearActors() ;
   }
   private void runAndClearActors(){

       runActors() ;

       _register.clearActors() ;
       _register.clearMonoflops() ;
   }

   private void executeSetterCommand( RosiSetterCommand command )throws Exception {

        _register.setSensorValue( command.getKey() , command.getValue() ) ;

        boolean shouldTrigger = _register.shouldTrigger( command.getKey() ) ;

        if( shouldTrigger )execute();

   }
   private void runActors(){

      if( isDebugMode() )log(_register.dumpActors() ) ;

      Map<String,String> actors = _register.getChangedActorValues() ;

      for( Map.Entry<String,String> actor : actors.entrySet() ){
         RosiCommand command = new RosiSetterCommand( actor.getKey() , actor.getValue() ) ;
         try{
            put(command); 
         }catch(Exception ie){
            errorLog("Warning : Error while sending command : "+ie.getMessage());
         }
      }
   }
   private void dumpRegister(){
 
      debug( "-- Register Dump --------------" );
      debug(_register.dumpRegisters() ); 
      debug("--------------------------------");
            
   } 
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){ }

   private ProgramExecutable 
               _loadExecutable(  String className , 
                                 ModuleContext context  )
      throws Exception
      {
      String commonErrorMessage = "Error initiating engine of class ("+className+") : " ;

      Class<? extends ProgramExecutable> s = null ;

      try{
         s = Class.forName( className ).
             asSubclass( org.rosi.modules.engine.ProgramExecutable.class ) ;
      }catch(Exception cnfe ){
         cnfe.printStackTrace();

         throw new
         IllegalArgumentException( "Error finding engine class : class not found : "+className);
      }
      Constructor<? extends ProgramExecutable> x = null ; 
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
