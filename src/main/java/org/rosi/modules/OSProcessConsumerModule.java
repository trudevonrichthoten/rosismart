package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.modem.*;


public class OSProcessConsumerModule extends RosiModule {

   private RosiCommandProcessor _commandProcessor = null ; 
   private String               _binaryPrefix     = null ;
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
   private File                 _file    = null ;
   private PatternTranslator    _filter  = null ;
   private Runtime              _runtime = Runtime.getRuntime() ;


   public OSProcessConsumerModule( String moduleName , ModuleContext context  )
      throws Exception {

      super(moduleName,context);

      _context = context ;

      log( "Started");
 
      _binaryPrefix = context.get("binaryPath" , true ); 

      log( "Context created with "+_binaryPrefix);

      String filterFile = context.get( "filterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("Filer file not found : "+ filterFile ) ;

         _filter = new PatternTranslator( f ) ;
      }


   } 
   public void run(){

       while(true){

          try{

             RosiCommand c  = take() ;
 
             if( ! ( c instanceof RosiSetterCommand ) ){
                 errorLog("Received an unexpected command type : "+c.getClass().getName() ) ;
                 continue ;
             }

             RosiSetterCommand command = new RosiSetterCommand( (RosiSetterCommand) c ) ; 

             debug("Command received : "+command);
             if( _filter != null ){

                String [] sub = _filter.translate( command.getKey()+":"+command.getValue() ) ;

                if( ( sub == null ) || ( sub.length == 0 ) )continue ;

                for( int i = 0 ; i < sub.length ; i++ ){
                  debug( "SUB["+i+"] : >"+sub[i]+"<");
                }
                int result = executeOSProcess( sub ) ;
                if( result != 0 )errorLog("executeOSProcess reported : "+result); 
 
             }

          }catch(InterruptedException ieee ){
             errorLog("Was interrupted and stopped" ) ;
             break ;
          }catch(Exception eee ){
             errorLog( "Got exeception in main loop: "+eee ) ;
             if( isDebugMode() )eee.printStackTrace();
             break ;
          }
       }

   }
   private int executeOSProcess( String [] args ){
    
	 if( ( ! args[0].startsWith("/") ) && ( _binaryPrefix != null ) )
              args[0] = _binaryPrefix+"/"+args[0] ;
	 try{ 
	    Process p =_runtime.exec( args ) ;
	    p.waitFor() ;
	    return p.exitValue() ;
	 }catch(Exception ee ){
	    errorLog("Got runtime execption : "+ee ) ;
	    return -1 ;
	 }
   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
