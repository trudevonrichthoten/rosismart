package org.rosi.util ;

import java.io.* ;
import java.util.* ;
import java.util.regex.* ;


public class PatternTranslator {



   private Pattern                _pattern ;
   private List<List<StackEntry>> _compilers ;
   private PatternTranslator   [] _configuration = null ;
   private File                   _configFile    = null ;
   private long                   _configFileTimestamp = 0 ;

   public PatternTranslator( File configFile ) throws Exception {

       _configFileTimestamp = configFile.lastModified() ;
       _configuration = loadConfig( _configFile = configFile ) ;

   }
   public synchronized boolean reload() throws Exception {

       long updated = _configFile.lastModified() ;

       if( updated == _configFileTimestamp )return false ;

       PatternTranslator [] config = loadConfig( _configFile ) ;

       _configFileTimestamp = updated ;
       _configuration       = config ;

       return true ;

   }
   public String [] translate( String in ) throws Exception {

       reload();

       PatternTranslator   [] conf = null ;
       synchronized(this) {
          if( ( conf = _configuration )  == null )return _translate( in ) ;
       }
       String [] result = null ;
       for( int j = 0 ; j < conf.length ; j++ ){

           if( ( result = conf[j]._translate( in ) ) != null )return result ;

       }
     
       return null ;
      
   } 
   public PatternTranslator( String pattern , String [] sourceCode ) throws Exception {
 
      _pattern   = Pattern.compile( pattern ) ;          
      _compilers = new ArrayList<List<StackEntry>>()  ;

      for( int i = 0 ; i < sourceCode.length ; i++ ){

        List<StackEntry> list = SimpleScanners.compile( sourceCode[i] )  ;

        _compilers.add( list ) ; 

      }
   
   }
   /**
     *  The actual translate
     *  --------------------
     *
     */
   public String [] _translate( String in ) throws Exception {

      Matcher m = _pattern.matcher( in ) ; 

      if( ! m.matches() )return null ;

      String [] results = new String[_compilers.size()] ;
      
      int i = 0 ;
      for( List<StackEntry> compiler : _compilers ){

         StringBuffer sb = new StringBuffer() ;

         for( StackEntry e : compiler ){

            if( ! e.isSubstitution() ){
                sb.append( e.getString() ) ;
            }else if( e.isNumber() ){
                int index = e.getNumber() ; 
                if( index > m.groupCount() )
                   throw new
                   IllegalArgumentException("Can't reference "+index+" (max is "+m.groupCount()+")");

                sb.append( m.group( e.getNumber() ) ) ;
            }else{
               throw new
               IllegalArgumentException("Substitution needs to be number; found : "+e.getString());
            }
           
         } 
	 results[i++] = sb.toString() ;
      } 
      return results ;
   }
   public static PatternTranslator [] loadConfig( File configFile ) throws Exception {

       String line = null ;
       int lineCount = 0 ;
       List<PatternTranslator> list = new ArrayList<PatternTranslator>() ;

       BufferedReader reader = new BufferedReader( new FileReader( configFile ) ) ;

       try{

          while( ( line = reader.readLine() ) != null ){

             line = line.trim() ;

             if( ( line.length() == 0 ) ||  line.startsWith("#" ) )continue ; 
  
             lineCount ++ ; 
          
             char []  b = { line.charAt(0) } ;
 
             String [] sp = line.split( new String( b )  ) ;

             if( ( sp.length < 3 ) || ( ( sp.length % 2 ) != 0 ) ) 
                throw new
                IllegalArgumentException(
                  "Config Error (line="+lineCount+") : Not enough tokens, or 'odd' number of delimiters in" ) ;

             String pattern = sp[1] ;
             String [] subs = new String[sp.length/2-1] ;
          
             for( int i = 0 ; i < (sp.length/2-1) ; i++ )subs[i] = sp[3+i*2] ;
 
             PatternTranslator translator = new PatternTranslator( pattern , subs ) ; 
             list.add( translator ) ;
          }

       }catch(Exception aii ){
          throw new 
          IllegalArgumentException("Config Error (line="+lineCount+") : "+aii.getMessage() );
       }finally{
          try{ reader.close() ; }catch(Exception ee ){ }
       }

       return list.toArray( new PatternTranslator[list.size()] ) ;
   }
   public static void main( String [] args ) throws Exception {

/*
      if( args.length < 1 ){
        System.out.println("Usage : ... <configFileName> <input> [<moreinput>]");
        System.exit(2);
      }
      List<StackEntry> list = SimpleScanners.compile( args[0] ) ;
      for( StackEntry e : list ){
         System.out.println(e.toString());
      }
*/
      if( args.length < 2 ){
        System.out.println("Usage : ... <configFileName> <input> [<moreinput>]");
        System.exit(2);
      }

      try{

          PatternTranslator p = new PatternTranslator( new File( args[0] ) ) ;

          for( int i = 1 ; i < args.length ; i++ ){

             String in = args[i] ;

             String [] result = p.translate( in ) ;

             System.out.println("Input : "+in ) ;
             if( result != null ){
                for( int j = 0 ; j < result.length ; j++ )
                System.out.println(" - "+result[j] );
             }
          }

      }catch( Exception  eeee ){
         System.out.println( eeee.getMessage() ) ;
      }
   }


}
