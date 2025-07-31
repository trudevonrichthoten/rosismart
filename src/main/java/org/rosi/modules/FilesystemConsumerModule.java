package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class FilesystemConsumerModule extends RosiModule {

   private RosiCommandProcessor _commandProcessor = null ; 
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
   private File                 _file    = null ;
   private PatternTranslator    _filter  = null ;


   public FilesystemConsumerModule( String moduleName , ModuleContext context  )
      throws Exception {

      super(moduleName,context);

      _context = context ;

      log( "Started");

      String flagDirectoryName = context.get("flagDirectory" , true );

      _file = new File( flagDirectoryName ) ;

      /* File needs to exist */
      if( ( ! _file.isDirectory()               ) || 
          ( ! _file.canWrite()  )    )
         throw new
         IllegalArgumentException( "Not a dir. or can't write to : "+_file ) ; 

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

             RosiSetterCommand command = new RosiSetterCommand( (RosiSetterCommand) c ) ; 

             if( _filter != null ){

                String [] sub = _filter.translate( command.getKey() ) ;

                if( ( sub != null ) && ( sub.length >=1 ) )command.setKey( sub[0] ) ;
                else continue ;

                sub = _filter.translate( command.getValue() ) ;
                if( ( sub != null ) && ( sub.length >=1 ) )command.setValue( sub[0] ) ;
 
             }

             File dest = new File( _file , command.getKey() ); 
             try{
                 PrintWriter writer  = 
                     new PrintWriter( new FileWriter( dest  ) ) ;

                 try{
                      writer.println(command.getValue()) ;
                 }finally{
                    try{ writer.close() ; }catch(Exception xxx ){}
                 }
             }catch(IOException ee ){
                 errorLog("Write failed : "+dest);
                 throw ee ;
             }


          }catch(InterruptedException ieee ){
             errorLog("Was interrupted and stopped" ) ;
             break ;
          }catch(Exception eee ){
             errorLog( "Got exeception in main loop: "+eee ) ;
             break ;
          }
       }

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
