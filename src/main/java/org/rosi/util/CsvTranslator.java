package org.rosi.util ;

import java.io.* ;
import java.util.* ;
import java.util.regex.* ;


public class CsvTranslator {

   /*
    * this is a more sloppy one.
    *
   private static Pattern _csvPattern = Pattern.compile("\"([^\"]*)\";") ;
    */
   private static Pattern _csvPattern = Pattern.compile("\"([^\"]*)\"[;]{0,1}") ;

   private BufferedReader _reader = null ;

   public CsvTranslator( Reader reader ) throws IOException {
      if( reader instanceof BufferedReader ){
          _reader = (BufferedReader)reader ;
      }else{
          _reader = new BufferedReader(reader); 
      }
   }
   public String [] readCsvLine() throws IOException {

       String line = _reader.readLine() ; 
 
       if( line == null )return null ;

       return getTokens( line ) ; 
   }

   public static String [] getTokens( String inString ) {

      List<String> list = new ArrayList<String>(); 

      Matcher m = _csvPattern.matcher( inString ) ; 

      while( m.find() ){

         for( int i = 1 ; i < (m.groupCount()+1) ; i++ )
         {
            list.add( m.group(i) ) ;
         }

      }

      return list.toArray( new String[list.size()] ) ;

   } 


   public static void main ( String [] args ) throws Exception {
       if( args.length < 1 ){
        System.out.println("Usage : ... <csvFile>" );
        System.exit(2);
      }

      File f = new File( args[0] ) ;

      if( ! f.canRead() ){
         System.err.println("File doesn't exist : "+args[0] );
         System.exit(4);
      }

      Reader reader = new BufferedReader( new FileReader( f ) ) ;

      String [] tokens = null ;

      try{
         CsvTranslator csvReader = new CsvTranslator( reader ) ;
         while( ( tokens = csvReader.readCsvLine() ) != null ){
            System.out.println("Lines : "+tokens.length);
         }
      }catch(IOException io ){
         System.err.println("IO Exception in reading file : "+io.getMessage());
      }finally{
         try{ reader.close() ; }catch(Exception eee ){}
      }

     
/*
       if( args.length < 2 ){
        System.out.println("Usage : ... <pattern> <input>" );
        System.exit(2);
      }
      String pattern = args[0] ;
      String inString = args[1] ;

      Pattern _pattern   = Pattern.compile( pattern ) ;          

      Matcher m = _pattern.matcher( inString ) ; 

      while( m.find() ){
          System.out.println("Next find ----------") ;

      for( int i = 0 ; i < (m.groupCount()+1) ; i++ )
      {
         System.out.println("["+i+"] "+m.group(i) );
      }

      }

*/

   }

}

