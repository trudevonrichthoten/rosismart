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

   
   public ModuleContext _context = null ;
   private RosiLogable   _log     = null ;
   /**
     *  Rosi Constructure with context.
     *
     */
   public  RosiModule( String name , ModuleContext context ){
       _name      = name ;
       _context   = context ;
       _log       = _context.getLogable() ;
   }
   /**
     * Returning the Module Name. 
     *
     */
   public String getName(){
       return _name ;
   }
   public  String getContext( String context ){
      return _context.get(context);
   }
   public boolean isDebugMode(){ return true ; } ;
   /**
     *  Debug message from Modules
     *
     */
   public void debug(String message){ 
      _log.debug(message);
    }
   /**
     *  Log message from Modules
     *
     */
   public void log(String message){ 
      _log.log(message);
    }
   /**
     *  Error message from Modules
     *
     */
   public void errorLog(String message){ 
      _log.errorLog(message);
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
