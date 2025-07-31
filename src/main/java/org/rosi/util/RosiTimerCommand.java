package org.rosi.util ;

import java.text.ParseException ;

public class RosiTimerCommand extends RosiCommand {
 
   private RosiCalendar _calendar = null ;
   public RosiTimerCommand(){
      super("timer");
      _calendar = new RosiCalendar() ;
   }
   public RosiTimerCommand( String time ) throws ParseException{
      super(time) ;
      _calendar = new RosiCalendar( time ) ;
   }

   public RosiCalendar getCalendar(){
      return _calendar ;
   }
   public String toString(){ 
      return _calendar.toString() ;
   }
}
