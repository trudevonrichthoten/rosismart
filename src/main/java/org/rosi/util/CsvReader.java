package org.rosi.util ;

import java.io.* ;
import java.util.* ;
import java.util.regex.* ;
import java.text.SimpleDateFormat ;
import java.text.ParseException ;

public class CsvReader extends BufferedReader  {

   public class AccountRecord implements Comparable<AccountRecord>{
      public float  amount ;
      public String unit ;
      public String subject ;
      public String detail ;
      public String peer ;
      public String IBAN ;
      public String SWIFT ;
      public Date   bookingDate ;
      public Date   validationDate ;
      public String localAccount ;
      public String info ;
      private int _hashCode = 0 ;
      public int compareTo( AccountRecord b ){
          
         long x = this.bookingDate.getTime() ;
         long y = b.bookingDate.getTime() ;
         if( x == y ){
             x = this.hashCode();
             y = b.hashCode();
             return x == y ? 0 : ( x < y ? -1 : 1 ) ;
         }else{
             return  x < y ? -1 : 1  ;
         }

      }
/*
      public int compare( AccountRecord a , AccountRecord b ){
         int x = a.hashCode() ;
         int y = b.hashCode() ;
         return x == y ? 0 : ( x < y ? -1 : 1 ) ;
      }
*/
      public boolean equals( Object obj ){
          return hashCode() == obj.hashCode() ;
      }
      public String toString(){
          StringBuffer sb = new StringBuffer(); 
          sb.append( hashCode() ).append(" ").
             append(_dateFormat.format(bookingDate)).append(" ").
             append(amount).append(" ").
             append(IBAN).append(" ").
             append(bookingDate).append(" ").
             append(validationDate).append(" ");
          return sb.toString();
      }
      public int hashCode(){
         if( _hashCode == 0 ){
            StringBuffer sb = new StringBuffer() ;
            sb.append(amount).append(unit).append(subject).append(detail).
               append(peer).append(IBAN).append(SWIFT).
               append(bookingDate.getTime()).append(validationDate.getTime()).
               append(localAccount).append(info) ;
            _hashCode = sb.toString().hashCode(); 
         } 
         return _hashCode ; 
      }
   } 

   private SimpleDateFormat _dateFormat = new SimpleDateFormat( "dd.MM.yy.kk" );

   public CsvReader( BufferedReader reader ) throws IOException {
      super(reader);
   }
   public String [] readCsvLine() throws IOException {

       String line = readLine() ; 
     
       if( line == null )return null ;

       return CsvTranslator.getTokens( line ) ; 
   }
   public AccountRecord readAccountRecord() throws IOException,ParseException  {

      AccountRecord record = new AccountRecord() ;

      String [] tokens = readCsvLine();
      if( tokens == null )return null ;
      
      if( tokens[1].length() == 5 )tokens[1] = tokens[1] + tokens[2].substring(5);
      else if( tokens[1].length() == 0 )tokens[1] = tokens[2] ;
      record.localAccount = tokens[0] ; 
      record.bookingDate    = _dateFormat.parse( tokens[1]+".12" ) ; 
      record.validationDate = _dateFormat.parse( tokens[2]+".12") ; 
      record.subject        = tokens[3] ;
      record.detail         = tokens[4] ;
      record.peer           = tokens[5] ;
      record.IBAN           = tokens[6] ;
      record.SWIFT          = tokens[7] ;
      record.amount         = Float.valueOf( tokens[8].replace(',','.') ) ;
      record.unit           = tokens[9] ;
      record.info           = tokens[10] ;

      record.hashCode() ; // to get it calculated.
      return record ;
   } 
   public Set<AccountRecord> load() throws IOException {

      CsvReader.AccountRecord record = null ;
      Set<AccountRecord> list = new TreeSet<AccountRecord>() ;

      for( int i = 0 ; true ; i++ ){

         try{
             if( i == 0 ){
                String [] t = readCsvLine();
                if( ( t.length < 11 ) || ( ! t[0].equals("Auftragskonto") ) )
                   throw new
                   IllegalArgumentException("Not an Account file");
             }else{
                record = readAccountRecord() ;
                if( record == null )break ;
                list.add(record); 
 
             }
         }catch(IOException eee ){
             throw eee ;
         }catch(Exception eee ){
             System.err.println(" Problem with : "+eee );
             eee.printStackTrace() ;
             continue ;
         }

      }
      return list ;

   }
   public static Set<AccountRecord> loadAccountFile( File accountFile ) throws Exception {

      if( ! accountFile.canRead() )
         throw new
         IOException("File not found or not accessible : "+accountFile ) ;

      CsvReader.AccountRecord record = null ;
      Set<AccountRecord> list = new TreeSet<AccountRecord>() ;

      CsvReader csvReader = new CsvReader( new BufferedReader( new FileReader( accountFile ) ) ) ;
      try{
         for( int i = 0 ; true ; i++ ){

            try{
                if( i == 0 ){
                   String [] t = csvReader.readCsvLine();
                   if( ( t.length < 11 ) || ( ! t[0].equals("Auftragskonto") ) )
                      throw new
                      IllegalArgumentException("Not an Account file");
                }else{
                   record = csvReader.readAccountRecord() ;
                   if( record == null )break ;
                   list.add(record); 
                }
            }catch(IOException eee ){
                throw eee ;
            }catch(Exception eee ){
                System.err.println(" Problem with : "+eee );
                eee.printStackTrace() ;
                continue ;
            }

         }
      }catch(IOException io ){
         System.err.println("IO Exception in reading file : "+io.getMessage());
      }finally{
         try{ csvReader.close() ; }catch(Exception eee ){}
      }

      return list ;
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
 
      CsvReader reader = new CsvReader( new BufferedReader( new FileReader( f ) ) ) ; 

      Set<AccountRecord> list = null  ;

      try{ 
         list = reader.load()  ;
      }finally {
          try{ reader.close() ; }catch(Exception ee ){}
      }

      for( AccountRecord  record : list ){
    
          System.out.println(" -> "+record);
      }
/*
      CsvReader csvReader = new CsvReader( new BufferedReader( new FileReader( f ) ) ) ;
      CsvReader.AccountRecord record = null ;
      try{
         while( true ){

            try{
                record = csvReader.readAccountRecord() ;
                if( record == null )break ;
                System.out.println("Record : "+record);
            }catch(IOException eee ){
                throw eee ;
            }catch(Exception eee ){
                System.err.println(" Problem with : "+eee );
                eee.printStackTrace() ;
                continue ;
            }
            
         }
      }catch(IOException io ){
         System.err.println("IO Exception in reading file : "+io.getMessage());
      }finally{
         try{ csvReader.close() ; }catch(Exception eee ){}
      }
*/
/*
      String [] tokens = null ;

      try{
         while( ( tokens = csvReader.readCsvLine() ) != null ){

            try{
               Date dd = d.parse(tokens[0]);
                System.out.println("Lines : "+tokens.length+"  : "+tokens[1]+" "+ dd );
            }catch(IOException eee ){
                throw eee ;
            }catch(Exception eee ){
                System.err.println(" Problem with : "+tokens[2]+" "+eee );
            }
            
         }
      }catch(IOException io ){
         System.err.println("IO Exception in reading file : "+io.getMessage());
      }finally{
         try{ csvReader.close() ; }catch(Exception eee ){}
      }
*/
     
   }

}

