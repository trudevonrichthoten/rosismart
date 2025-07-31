package org.rosi.util ;

import java.util.regex.* ;
import java.util.* ;
import java.io.*;


public class ConfigInterpreter {

   private static String __globalSectionName = "GLOBAL" ;

   private Pattern _assignment = Pattern.compile( "[ ]*([0-9a-zA-Z]*)=(.*)[ ]*" );
   private Pattern _header     = Pattern.compile( "[ ]*\\[(.*)\\].*" );

   private File               _configFile ;
   private Map<String,Map<String,String>> _config ;
/*
   public class ConfigEntry {
      private String             _name = null ;
      private Map<String,String> _map  = new HashMap<String,String>() ;
      public ConfigEntry( String name ){ this._name = name ; }
      public void add( String key , String value ){

         if( ( key == null ) || ( value == null ) )
           throw new 
           IllegalArgumentException("'null' key or value not allowed");

         _map.put( key , value ) ;

      }
      public String toString(){
         StringBuffer sb = new StringBuffer() ;
         sb.append(_name).append("\n");
         for( Map.Entry<String,String> entr : _map.entrySet() ){
            sb.append(entr.getKey()).append(" -> ").append(entr.getValue()).append("\n") ; 
         }
         return sb.toString();
      }
      public String getName(){ return _name ; }
   }
*/
   public ConfigInterpreter( File configFile ) throws Exception {
 
      _configFile   = configFile ;
      _config       = execFile( _configFile ) ;

   }
   public void dumpConfig(){

       for( Map.Entry<String,Map<String,String>> section :  _config.entrySet() ){
          System.out.println(section.getKey());
          for( Map.Entry<String,String> entry : section.getValue().entrySet() ){

             System.out.println("   "+entry.getKey() + " -> "+entry.getValue() );

          }
       }
      
   }
   private Map<String,Map<String,String>> execFile( File file ) throws Exception {

      BufferedReader br = new BufferedReader( new FileReader( file ) ) ;
      Map<String,Map<String,String>> map = new HashMap<String,Map<String,String>>() ; 
      try{

         String sectionName   = __globalSectionName ;
         Map<String,String> e = new HashMap<String,String>() ;      

         map.put( sectionName , e ) ;

         String line = null ;
         while( ( line = br.readLine() ) != null ){

             line = line.trim() ;
             if( ( line.length() == 0 ) || line.startsWith("#") )continue ;
 
             Matcher m = _header.matcher( line );
             if( m.matches() ){

                sectionName = m.group(1) ;

                if( ( e = map.get( sectionName ) ) == null )
                   map.put( sectionName , e = new HashMap<String,String>() ) ; 

                continue ;
             }
             m = _assignment.matcher( line );
             if( m.matches() ){
                String key   = m.group(1) ;
                String value = _resolve( map , e ,  m.group(2) ) ;
                e.put( key , value ) ;
                continue ;
             }
         }
      }catch(Exception e ){
         throw e ;
      }finally{
         try{ br.close() ; }catch(Exception ioe ){}
      }
      return map ;
   }
   private String _resolve( Map<String,Map<String,String>> base , 
                            Map<String,String> section ,
                            String value ) throws Exception {

      List<StackEntry> stack = SimpleScanners.compile( value ) ; 
      StringBuffer sb = new StringBuffer();
      for( StackEntry se : stack ){
         String find = se.isNumber() ? ""+se.getNumber() : se.getString() ;  
         if( se.isSubstitution() ){
            String sub = findVariable( base , section , find ) ;
            sb.append( sub == null ? "(?)" : sub ) ;
         }else{
            sb.append( find ) ;
         }
      }
      return sb.toString();
   }
   public Map<String,String> getSection( String name ){
      return _config.get(name);
   } 
   public String get( String name ){
     return  findVariable( _config , _config.get(__globalSectionName) , name ) ; 
   }
   private String findVariable(
           Map<String,Map<String,String>> base ,
           Map<String,String> section , 
           String variableName ){

        String [] sec = variableName.split("\\.");
        
        if( sec.length == 1 ){
           String h = section.get( variableName ) ;
           Map<String,String> g = base.get(__globalSectionName);
           return g.get( variableName );
        }else{
           Map<String,String> s = base.get( sec[0] ) ;
           if( s == null )return null ;
           return( s.get( sec[1] ) ) ;
        } 
   }
   public static void main( String [] args ) throws Exception {
     
     if( args.length < 1 ){
        System.err.println("Usage : ... <filename>");
        System.exit(4);
     }
     ConfigInterpreter ci = new ConfigInterpreter( new File( args[0] ) ) ;       
     ci.dumpConfig() ;     
   }

}
