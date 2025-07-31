package org.rosi.modules ;

import java.util.* ;
import java.util.regex.* ;
import java.text.SimpleDateFormat ;
import java.time.LocalDateTime ;
import java.time.LocalDate ;
import java.time.format.DateTimeFormatter ;
import java.io.* ;

import com.mongodb.* ;
import com.mongodb.client.* ;
import com.mongodb.client.result.* ;
import org.bson.*;

import org.rosi.util.*;
import org.rosi.execution.*;

public class MongoConsumerModule extends RosiModule {

   private RosiCommandProcessor _commandProcessor = null ; 
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");
   private DateTimeFormatter    _thisHour  = DateTimeFormatter.ofPattern( "yyyy_MM_dd_HH" ) ;
   private DateTimeFormatter    _thisDay   = DateTimeFormatter.ofPattern( "yyyy_MM_dd" ) ;
   private DateTimeFormatter    _thisWeek  = DateTimeFormatter.ofPattern( "yyyy__ww" ) ;
   private DateTimeFormatter    _thisMonth = DateTimeFormatter.ofPattern( "yyyy_MM" ) ;
   private DateTimeFormatter    _dateFormat = null ;

   private static String        _number_pattern_string   = "[0-9]*(\\.[0-9]*)?" ;
   private Pattern              _numberPattern           = Pattern.compile( _number_pattern_string ) ;

   private MongoClient               _mongoClient = null ;
   private MongoDatabase             _database    = null ;
   private MongoCollection<Document> _collection  = null ;

   private Map<String,Object>  _map = new HashMap<String,Object>() ;

   private long _updateMillis = 60000L ;
   private int  _currentUnit  = -1 ;

   enum NameMode {
      NAME , HOUR,  DAY , WEEK , MONTH 
   };
   private NameMode _nameMode = NameMode.MONTH ;

   public MongoConsumerModule( String moduleName , ModuleContext context  )
      throws IllegalArgumentException, IOException {

      super(moduleName,context);

      _context = context ;

      log( "Started");

      String mongoDbHostname   = context.get("dbHostname"   , false );
      String mongoDbPort       = context.get("dbPort"       , false );
      String mongoDbName       = context.get("dbName"       , true );
      String mongoDbCollection = context.get("dbCollection" , true );

      mongoDbHostname = mongoDbHostname == null ? "localhost" : mongoDbHostname ;
      mongoDbPort     = mongoDbPort     == null ? "27017"     : mongoDbPort ;

      int port = 0 ;
      try{
          port = Integer.parseInt( mongoDbPort ) ;
      }catch( NumberFormatException ine ){
          String err = "Illegal Number format for port : "+mongoDbPort ;
          errorLog(err);
          throw new
             IllegalArgumentException(err) ;
      }

      log("Mongo Db Connection : "+mongoDbHostname +" (" +port+" )" ) ;
      log("Mongo Db            : "+mongoDbName +" (" +mongoDbCollection+" )" );

      String uri = "mongodb://"+mongoDbHostname+":"+port ;
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("testdb");
            System.out.println("Connected to DB: " + database.getName());
        }

      _mongoClient  = MongoClients.create(uri) ;
//      _mongoClient  = new MongoClient( mongoDbHostname , port );
      _database     = _mongoClient.getDatabase( mongoDbName );
      _collection   = null ;
      if( mongoDbCollection.equals( "month" ) ){
         _collection = null ;
         _nameMode   = NameMode.MONTH ;
         _dateFormat = _thisMonth ;
      }else if( mongoDbCollection.equals( "day" ) ){
         _collection = null ;
         _nameMode   = NameMode.DAY ;
         _dateFormat = _thisDay ;
      }else if( mongoDbCollection.equals( "week" ) ){
         _collection = null ;
         _nameMode   = NameMode.WEEK ;
         _dateFormat = _thisWeek ;
      }else if( mongoDbCollection.equals( "hour" ) ){
         _collection = null ;
         _nameMode   = NameMode.HOUR ;
         _dateFormat = _thisHour ;
      }else{
         _collection   = _database.getCollection( mongoDbCollection );
         _nameMode     = NameMode.NAME ;
         _dateFormat   = null ;
      }

   } 
   private int getUnitValue( LocalDateTime ldt ){
     switch( _nameMode ){
          case MONTH : return ldt.getMonthValue() ;
          case WEEK  : return (int) (ldt.getDayOfYear() / 7 ) ;
          case DAY   : return ldt.getDayOfYear() ;
          case HOUR  : return ldt.getHour() ;
          case NAME  : return -1 ;
     }
     return -1 ; 
   }
   private void updateDb( Map<String,Object> map ) throws Exception {

       Document doc = new Document( map ) ;
       doc.append( "timestamp" , new Date() ) ;

       if( _nameMode != NameMode.NAME ){
          LocalDateTime ldt = LocalDateTime.now() ;
          int unit = getUnitValue( ldt ) ;
          if( ( _collection == null ) || ( unit != _currentUnit ) ){
             _currentUnit  = unit ;
             _collection   = _database.getCollection( "logger_"+ldt.format( _dateFormat ) ) ;
          }

       }
       _collection.insertOne(doc);

   }
   public void run(){
     try{
       long lastUpdate = 0L ;
       boolean _stringOnly = false ;
       while(true){

          try{

             RosiCommand command  =  take() ;

             if( _stringOnly ){
                 _map.put( command.getKey().replace( '.' , '/' ) , command.getValue() ) ;
             }else{
                 String key   = command.getKey().replace( '.' , '/' ) ;
                 String value = command.getValue();
                 if( value.equalsIgnoreCase( "on" ) ){
                    _map.put( key , true ) ;
                 }else if( value.equalsIgnoreCase( "off" ) ){
                    _map.put( key , false ) ;
                 }else if( _numberPattern.matcher( value ).matches() ){ 
                    _map.put( key , Double.parseDouble( value ) ) ;
                 }else{
                    _map.put( key , command.getValue() ) ;
                 }
             }
   
 
             long now = System.currentTimeMillis() ;
             if( ( now - lastUpdate )  > _updateMillis ){
               updateDb( _map ) ;
               lastUpdate = now ;
             }

          }catch(InterruptedException ieee ){
             errorLog("Was interrupted and stopped" ) ;
             throw ieee ;
          }catch(Exception eee ){
             errorLog( "Stopping due to exeception in main loop: "+eee ) ;
             // to avoid endless loops, we stop.
             throw eee;
          }
       }
     }catch(Exception iee ){
     }finally{
         log("Mongo db loop ended");
     }

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
