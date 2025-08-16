package org.rosi.execution ;

import java.io.* ;
import java.util.* ;
import java.util.concurrent.* ;
import java.lang.reflect.* ;
import java.text.SimpleDateFormat;

import org.rosi.util.* ;
import org.rosi.modules.* ;

public class Switchboard  {

   private class LogManager  {

      private static final int LOG_ERROR = 1 ;
      private static final int LOG_INFO  = 2 ;
      private static final int LOG_DEBUG = 4 ;

      private final SimpleDateFormat _sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");

      private int    _logLevel = LOG_ERROR ;
      private String _tag      = null ;

      /* Not using this class for now. */
      
      public class RosiPrintStream extends PrintStream {
         private String _name ;
         public RosiPrintStream( OutputStream out , String name ){
            super(out);
            _name = name ;
         }
         public void println( String in ){
            super.println(getFormattedDate()+" ["+_name+"] "+in);
         }
      }
      private PrintStream _stdout = System.out ;
      private PrintStream _stderr = System.err ;

      private class LogableImpl implements RosiLogable {
         private String _tag      = "" ;
         private int    _logLevel = 0 ;
         private LogableImpl( String tag , int logLevel ){
            this._tag      = tag ;
            this._logLevel = logLevel ;
         }
         public void debug(String message){ 
            if( ( this._logLevel & LOG_DEBUG ) != 0 )_stdout.println(getFormattedDate()+" ["+this._tag+"] (debug) "+message);
         }
         public void log(String message){ 
            if( ( this._logLevel & LOG_INFO ) != 0 )_stdout.println(getFormattedDate()+" ["+this._tag+"] "+message);
         }
         public void errorLog(String message){ 
            if( ( this._logLevel & LOG_ERROR ) != 0 )
            if( _stdout != _stderr )_stderr.println(getFormattedDate()+" ["+this._tag+"] (error) "+message);
         }
 
      }
      public RosiLogable getLogable( String tag ){
         return new LogableImpl( tag , this._logLevel ) ;
      }
      public RosiLogable getLogable(){
         return new LogableImpl( _tag , this._logLevel ) ;
      }
      private LogManager( String tag , String stdoutName , String stderrName , String logLevel ) throws IOException {

         this._tag = tag ;

         if( stdoutName != null ){
            _stdout = new PrintStream( new FileOutputStream( new File( stdoutName ) , true ) ) ;
            System.setOut( new RosiPrintStream( _stdout , "stdout" ) ) ;
         }
         if( stderrName != null ){
            _stderr = new PrintStream( new FileOutputStream( new File( stderrName ) , true ) ) ;
            System.setErr( new RosiPrintStream( _stderr , "stderr" ) ) ;
         }
         if( logLevel != null ){
            if( logLevel.equals("debug") ){
               _logLevel = LOG_INFO | LOG_ERROR | LOG_DEBUG ;
            }else if( logLevel.equals("info") ){
               _logLevel = LOG_INFO | LOG_ERROR ;
            }else if( logLevel.equals("none") ){
               _logLevel = 0 ;
            }else{
               _logLevel = LOG_ERROR ;
            }
         }
      }
      public boolean isDebugMode(){
         return ( _logLevel & LOG_DEBUG ) != 0 ;
      }
      public String getFormattedDate(){
         return _sdf.format(new Date());
      }
   }
   private ConfigInterpreter _config  = null ;
   private  LogManager       _printer = null ;
   private RosiLogable       _log     = null ;

   public Switchboard( String configFileName ) throws Exception {

      _config = new ConfigInterpreter( new File( configFileName ) ) ;

      _printer = new LogManager( "Switch",
                                 _config.get("stdout" ) ,
                                 _config.get("stderr" ) ,
                                 _config.get("logging") ) ;
      _log = _printer.getLogable() ;
      /*
       * Which modules to launch 
       * ........................
       */
      String launchModuleString = _config.get( "launch" ) ;
      if( launchModuleString == null )
         throw new
         IllegalArgumentException("'launch' variable not found in config file");
        
      String [] launchModules = launchModuleString.split(",") ;

      BlockingQueue<RosiCommand> queue = new ArrayBlockingQueue<RosiCommand>(128) ;

      Map<String,RosiModule> moduleMap = new HashMap<String,RosiModule>() ;

      /**
        * Initiate all modules, listed in the 'launch' directive.
        * -------------------------------------------------------
        *
        */
      for( int i = 0 ; i < launchModules.length ; i++ ){

         String moduleName = launchModules[i].trim() ;
         if( moduleName.equals("") )continue ;

         _log.log("Preparing module ; "+moduleName); 

         Map<String,String> contextMap = _config.getSection(moduleName) ; 

         if( contextMap == null ){
             _log.errorLog("Error : no section for module found (skipping): "+moduleName);
             continue ;
         }
         ModuleContext context = new ModuleContext( moduleName , contextMap ) ;
         context.setLogable( _printer.getLogable(moduleName) ) ;
         /*
          *  Load the RosiModule
          *  -------------------
          */
         String className = context.get( "inputClass" ) ;
         if( className == null ){
             _log.errorLog("Error : 'inputClass' not specified for module : "+moduleName);
             continue ;
         }

         try{
            RosiModule module = _loadRosiModule( moduleName , className , context ) ;

            String processorName = context.get( "processorClass" ) ;
            if( processorName == null ){
               _log.debug("No command processor specified for "+moduleName);
            }else{
               RosiCommandProcessor processor = _loadRosiCommandProcessor( processorName , context ) ;
               if( processor != null )module.setCommandProcessor( processor ) ;
            }
            moduleMap.put( moduleName , module ) ; 

         }catch(Exception ee ){
            _log.errorLog("Error in initializing module : "+moduleName+" -> "+ee);
            ee.printStackTrace() ;
         }
      }
      /**
        * Contruction of the 'queue network'.
        *
        */
      constructModuleCommunicationNetwork( moduleMap ) ;

      startRunnables( launchModules , moduleMap ) ;

   }
   private void constructModuleCommunicationNetwork( Map<String,RosiModule> moduleMap ){
  
      for( Map.Entry<String,RosiModule> moduleEntry : moduleMap.entrySet() ){

         RosiModule module = moduleEntry.getValue() ;

         String senders = module.getContext().get("receiveFrom") ;
         if( senders != null ){ 
            String [] s    = senders.split(",");
            for( int i = 0 ; i < s.length ; i++ ){
               String senderName = s[i].trim() ;
               if( senderName.equals("") )continue ;

               RosiModule sender = moduleMap.get(senderName) ;
               if( sender == null )
                 throw new
                 IllegalArgumentException("'receiveFrom: Sender not found : "+senderName );

               _log.log( "'"+sender.getName()+
                         "'.addToSenderQueueList('"+module.getName()+"'.getReceiverQueue() )" );  

               sender.addToSenderQueueList( module.getReceiverQueue() ) ;
            }
         }

         String receivers = module.getContext().get("sendTo") ;
         if( receivers == null )continue ;
         String [] r    = receivers.split(",");
         for( int i = 0 ; i < r.length ; i++ ){
            String receiverName = r[i].trim() ;
            if( receiverName.equals("") )continue ;

            RosiModule receiver = moduleMap.get(receiverName) ;
            if( receiver == null ){
               _log.errorLog("Warning : receiver not found : "+receiverName);
               continue ;
            }
            _log.log("'"+module.getName()+"'.addToSenderQueueList('"+receiver.getName()+"'.getReceiverQueue() )" );  

            module.addToSenderQueueList( receiver.getReceiverQueue() ) ;
         }
 
      }

   }

   private void startRunnables( String [] launchModules , Map<String,RosiModule> moduleMap ){

      for( int i = 0 ; i < launchModules.length ; i++ ){

         String moduleName = launchModules[i].trim() ;
         if( moduleName.equals("") )continue ;

         RosiModule module = moduleMap.get(moduleName) ;
         if( module == null )continue ;
  
         new Thread(module).start() ;
 
         _log.log(moduleName+" started");
      }
     
   }
   private RosiCommandProcessor _loadRosiCommandProcessor( 
                   String processorName ,
                   ModuleContext context  
   ){

      Class<? extends RosiCommandProcessor> p = null ;

      try{
         p = Class.forName( processorName ).
             asSubclass( org.rosi.modules.RosiCommandProcessor.class ) ;
      }catch(Exception cnfe ){
         throw new
         IllegalArgumentException( 
            "Error initiating processor : "+processorName+
            " : ("+cnfe.getClass().getName()+") "+cnfe.getMessage() 
         );
      }
      Constructor<? extends RosiCommandProcessor> x = null ; 
      try{

          x = p.getConstructor( org.rosi.modules.ModuleContext.class  );

      }catch(NoSuchMethodException nsme ){
         throw new
         IllegalArgumentException( 
            "Error initiating processor : "+processorName+
            " : ("+nsme.getClass().getName()+") "+nsme.getMessage() 
         );
      }

      try{

         return  x.newInstance( context ) ;
      
      }catch(InvocationTargetException ie ){
         Throwable t = ie.getTargetException() ;
         throw new
         IllegalArgumentException( 
            "Error initiating processor : "+processorName+
            " : ("+t.getClass().getName()+") "+t.getMessage() 
         );
      }catch(Exception ee ){
         throw new
         IllegalArgumentException( 
            "Error initiating processor : "+processorName+
            " : ("+ee.getClass().getName()+") "+ee.getMessage() 
         );
      }
   } 
   private RosiModule _loadRosiModule( String moduleName ,
                                       String className , 
                                       ModuleContext context )
      throws Exception
      {

      Class<? extends RosiModule> s = null ;

      try{
         s = Class.forName( className ).
             asSubclass( org.rosi.modules.RosiModule.class ) ;
      }catch(Exception cnfe ){
         throw new
         IllegalArgumentException( "Error initiating module : "+moduleName+" : class not found : "+className);
      }
      Constructor<? extends RosiModule> module = null ; 
      try{
          module = s.getConstructor( 
                  java.lang.String.class ,
                  org.rosi.modules.ModuleContext.class
                              );
      }catch(NoSuchMethodException nsme ){
         throw new
         IllegalArgumentException( "Error initiating module : "+moduleName+" : "+nsme.getMessage());
      }
      
      try{

         return module.newInstance( moduleName , context ) ;
      
      }catch(InvocationTargetException ie ){
         Throwable t = ie.getTargetException() ;
         if( t instanceof RosiRuntimeException )
          throw ((RosiRuntimeException)t);

         throw new
         IllegalArgumentException( 
            "Error initiating module : "+moduleName+
            " : ("+t.getClass().getName()+") "+t.getMessage() 
         );
      }catch(Exception ee ){
         throw new
         IllegalArgumentException( 
            "Error initiating module : "+moduleName+
            " : ("+ee.getClass().getName()+") "+ee.getMessage() 
         );
      }
   }
   public static void main( String [] args ) throws Exception {

      if( args.length < 1 ){
          System.err.println("Usage : ... <configFile>");
          System.exit(3);
      }
      try{
         new Switchboard( args[0] ) ;
      }catch(RosiRuntimeException rre){
         System.err.println("Error : "+rre.getMessage());
      }catch(Exception ioe){
         System.err.println("Couldn't start : "+ioe.getMessage() );
         throw ioe;
      }

   }
}
