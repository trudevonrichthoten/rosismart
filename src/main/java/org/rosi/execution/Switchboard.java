package org.rosi.execution ;

import java.io.* ;
import java.util.* ;
import java.util.concurrent.* ;
import java.lang.reflect.* ;

import org.rosi.util.* ;
import org.rosi.modules.* ;

public class Switchboard implements RosiLogable {

   private ConfigInterpreter _config = null ;
   private PrintStream       _stdout = System.out ;
   private PrintStream       _stderr = System.err ;

   public void log( String message ){
      this.logInfo(message);
   }
   public void errorLog( String message ){
      this.errorLog(message);
   }
   public void debug( String message ) {
      this.logInfo(message);
   }
   private void logInfo( String msg ){
     System.out.println("Switchboard: "+msg);
   }
   private void logError( String msg ){
     System.err.println("Switchboard: Error "+msg);
   }
   private void setPrintStreams( PrintStream out , PrintStream err ){
     _stdout = out ;
     _stderr = err ;
   }
   public Switchboard( String configFileName ) throws Exception {

      _config = new ConfigInterpreter( new File( configFileName ) ) ;

      /*
       * Replace stdout and stderr
       * ........................
       */
      String printStreamString = _config.get("stdout") ;
      if( printStreamString != null ){
         PrintStream stdout = new PrintStream( new FileOutputStream( new File( printStreamString ) , true ) ) ;
         System.setOut( stdout ) ;
      }
      printStreamString = _config.get("stderr") ;
      if( printStreamString != null ){
         PrintStream stderr = new PrintStream( new FileOutputStream( new File( printStreamString ) , true ) ) ;
         System.setErr( stderr ) ;
      }
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

         logInfo("Preparing module ; "+moduleName); 

         Map<String,String> contextMap = _config.getSection(moduleName) ; 

         if( contextMap == null ){
             logError("Error : no section for module found : "+moduleName);
             continue ;
         }
         ModuleContext context = new ModuleContext( moduleName , contextMap ) ;
         /*
          *  Load the RosiModule
          *  -------------------
          */
         String className = context.get( "inputClass" ) ;
         if( className == null ){
             logError("Error : 'inputClass' not specified for module : "+moduleName);
             continue ;
         }
         /* temporary setting output to switchboard, resetting after  module is created. */
         context.setLogable( this ) ;
         RosiModule module = _loadRosiModule( moduleName , className , context ) ;
         if( module == null ){
             logError("Error : Couldn't launch : "+className);
             continue ;
         }
         context.setLogable( module ) ;
         String processorName = context.get( "processorClass" ) ;
         if( processorName == null ){
//             logInfo("No command processor specified "+moduleName);
         }else{
  
             RosiCommandProcessor processor = _loadRosiCommandProcessor( processorName , context ) ;
             if( processor != null )module.setCommandProcessor( processor ) ;

         }
         moduleMap.put( moduleName , module ) ; 

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

               logInfo( "'"+sender.getName()+
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
               System.out.println("Warning : receiver not found : "+receiverName);
               continue ;
            }
            logInfo("'"+module.getName()+"'.addToSenderQueueList('"+receiver.getName()+"'.getReceiverQueue() )" );  

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
 
         logInfo(moduleName+" started");
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
