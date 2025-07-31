package org.rosi.modules ;

import java.util.concurrent.ArrayBlockingQueue ;
import java.util.concurrent.BlockingQueue ;
import org.rosi.util.RosiCommand ;
import org.rosi.util.RosiLogable ;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Date ;
import java.text.SimpleDateFormat ;
import java.io.PrintStream;
import java.io.OutputStream;

public abstract class RosiModule implements Runnable , RosiLogable {

   private String  _name = null ;

   private BlockingQueue<RosiCommand>
            _receiverQueue   = null ;

   private List<BlockingQueue<RosiCommand>> 
            _senderQueueList = new ArrayList<BlockingQueue<RosiCommand>>() ;

   private static final int LOG_ERROR = 1 ;
   private static final int LOG_INFO  = 2 ;
   private static final int LOG_DEBUG = 4 ;

   private int _logLevel = LOG_ERROR ;

   private RosiPrintStream _rosiPrint = null ;

   private SimpleDateFormat _sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");

   public class RosiPrintStream extends PrintStream {
       public RosiPrintStream( OutputStream out ){
          super(out);
       }
       public void println( String in ){
          super.println(_name+" "+in);
       }
   }
   public ModuleContext _context = null ;
   /**
     *  Rosi Constructure with context.
     *
     */
   public  RosiModule( String name , ModuleContext context ){
       _name      = name ;
       _context   = context ;
       _rosiPrint = new RosiPrintStream(System.out);

       String loglevel = _context.get("logLevel") ;
       if( loglevel != null ){
          if( loglevel.equals("debug") ){
             _logLevel = LOG_INFO | LOG_ERROR | LOG_DEBUG ;
          }else if( loglevel.equals("info") ){
             _logLevel = LOG_INFO | LOG_ERROR ;
          }else if( loglevel.equals("none") ){
             _logLevel = 0 ;
          }else{
             _logLevel = LOG_ERROR ;
          }
       }
   }
   /**
     * Returning the Module Name. 
     *
     */
   public String getName(){
       return _name ;
   }
   public PrintStream getRosiPrintStream(){
       return _rosiPrint;
   }
   public  String getContext( String context ){
      return _context.get(context);
   }
   public String getFormattedDate(){
      return _sdf.format(new Date());
   }
   /**
     *  Debug message from Modules
     *
     */
   public void debug(String message){ 
      if( ( _logLevel & LOG_DEBUG ) != 0 )System.out.println(getFormattedDate()+" "+_name+" "+message);
    }
    public boolean isDebugMode(){
      return ( _logLevel & LOG_DEBUG ) != 0 ;
    }
   /**
     *  Log message from Modules
     *
     */
   public void log(String message){ 
      if( ( _logLevel & LOG_INFO ) != 0 )System.out.println(getFormattedDate()+" "+_name+" "+message);
    }
   /**
     *  Error message from Modules
     *
     */
   public void errorLog(String message){ 
      if( ( _logLevel & LOG_ERROR ) != 0 )System.err.println(getFormattedDate()+" "+_name+" "+message);
   }
   /**
     * Returning the Module Context.
     *
     */
   public ModuleContext getContext(){ return _context ; }
   /**
     * Returning the Module Context.
     *
     */
   public abstract void setCommandProcessor( RosiCommandProcessor processor ) ;
   /**
     * Returns the reveiver queu of this module.
     *
     */
   public synchronized BlockingQueue<RosiCommand> getReceiverQueue(){
     if( _receiverQueue == null )_receiverQueue = new ArrayBlockingQueue<RosiCommand>(128);
     return _receiverQueue ;
   }
   /**
     * Adding a queue to the list of senders.
     *
     */
   public synchronized void addToSenderQueueList( BlockingQueue<RosiCommand> queue ) {
      for( BlockingQueue<RosiCommand> cursor : _senderQueueList ){
         if( cursor == queue ){
            System.out.println("Duplicate queue found in sender list, skipping");
            return ;
         }
      }
      _senderQueueList.add(queue);
   }
   /**
     * Sending a command to all receiver queues.
     *
     */
   public void put( RosiCommand command )throws Exception {
      command.setSource(getName());
      for( BlockingQueue<RosiCommand> queue : _senderQueueList ){
          queue.put( command ) ;
      }
   }
   public boolean mightBlock(){
       return _receiverQueue.size() == 0 ;
   }
   /**
     * Wait for 
     *
     */
   public RosiCommand take() throws Exception {
       if( _receiverQueue != null ) return _receiverQueue.take() ; 
       throw new
       IllegalArgumentException( "Module '"+getName()+"' doesn't have a receiver queue.");
   }
}
