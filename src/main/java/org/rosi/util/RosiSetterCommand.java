package org.rosi.util ;


public class RosiSetterCommand extends RosiCommand {
 
   public RosiSetterCommand( String key , String value ){
      super(key,value) ;
   }
   public RosiSetterCommand( RosiSetterCommand command ){
//      super( command.getKey() , command.getValue());
        super( command ) ;
   }
}
