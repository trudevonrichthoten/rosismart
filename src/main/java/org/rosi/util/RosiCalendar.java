package org.rosi.util ;

import java.util.Date ;
import java.util.Calendar ;
import java.text.SimpleDateFormat ;
import java.text.ParseException ;

public class RosiCalendar {

   private Calendar _calendar    = Calendar.getInstance() ;
   private SimpleDateFormat _sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

   public RosiCalendar( String dateString ) throws ParseException {

       Date ourTime = _sdf.parse( dateString );
       
       _calendar.setTime( ourTime ) ;
             
   }
   public RosiCalendar(){
       _calendar.setTime( new Date() ) ;
   }
   public long minutesOfDay(){ 
      return _calendar.get(Calendar.HOUR_OF_DAY) * 60 + _calendar.get(Calendar.MINUTE) ; 
   }
   public int getDayOfWeek(){ 
      return  _calendar.get(Calendar.DAY_OF_WEEK) ; 
   }
   public long getTimeInMillis(){ 
      return _calendar.getTimeInMillis() ; 
   }
   public String toString(){
      return _sdf.format(  _calendar.getTime() ) ;
   }

}
