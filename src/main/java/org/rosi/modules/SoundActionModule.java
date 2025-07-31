package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.modem.*;


public class SoundActionModule extends RosiModule {

   private RosiCommandProcessor _commandProcessor = null ; 
   private String               _execName         = null ;
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
   private File                 _file    = null ;
   private PatternTranslator    _filter  = null ;


   public SoundActionModule( String moduleName , ModuleContext context  )
      throws Exception {

      super(moduleName,context);

      _context = context ;

      log( "Started");
 
      _execName = context.get("executable" , true ); 

      log( "Context created with "+_execName);

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

             debug("Sound received : "+command);
             if( _filter != null ){

                String [] sub = _filter.translate( command.getKey()+":"+command.getValue() ) ;

                if( ( sub == null ) || ( sub.length == 0 ) )continue ;

                for( int i = 0 ; i < sub.length ; i++ ){
                  debug( "SUB["+i+"] : >"+sub[i]+"<");
                }
                int result = playSound( sub[0] ) ;
                if( result != 0 )errorLog("Play sound reported : "+result); 
 
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
   private Runtime _runtime     = Runtime.getRuntime() ;
   private int playSound( String sentence ){
    
	 String [] x = new String[2] ; 
	 x[0] = _execName ;
	 x[1] = sentence ;
	 
	 try{ 
	    Process p =_runtime.exec( x ) ;
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
