package org.rosi.modules.engine;

import org.rosi.modules.ModuleContext ;
import org.rosi.modules.engine.InspectionCompanion.FieldDetails;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap ;
import java.util.HashSet;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Collections;
import java.lang.reflect.*;


public class SimpleJavaEngine implements BasicEnginable, Runnable {

/* ------------------------------------------------------------------ 
 ____  _                   _                  
/ ___|| |_ _ __ _   _  ___| |_ _   _ _ __ ___ 
\___ \| __| '__| | | |/ __| __| | | | '__/ _ \
 ___) | |_| |  | |_| | (__| |_| |_| | | |  __/
|____/ \__|_|   \__,_|\___|\__|\__,_|_|  \___|

*/
    /*
     *   Available Annotations:
     * 
     *   device(<options>)
     *      <options>:
     *          triggerOnChange: returns true on 'setValue'
     *          report: behaves like modified actor
     *   sensor(<option>)
     *      <options> : (inherits from 'device')
     *   actor(<option>)
     *      <options> : ninherits from 'device')
     *   mono(<target>,<delay>,<options>)
     *      <target> : name of the sensor to be observed.
     *      <delay>: delay in seconds or keyword 'descriminator'
     *      <options>:  (inherits from 'device')
     *          noRetrigger
     *          triggerOnSourceRepeats
     */
    class MotionDetector {

        String cover = "closed" ;
        int brightness = 0 ;

        @DeviceAnnotation("sensor(triggerOnChange)")
        String motionCount = "count_1" ;

        @DeviceAnnotation("mono(motionCount,3,triggerOnChange,noRetrigger,report)")
        boolean motionCount_mono = false ;

        @DeviceAnnotation("actor()")
        boolean motionCount_actor = false ;

        @DeviceAnnotation("sensor(triggerOnChange)")
        String battery = "high" ;

        @DeviceAnnotation("mono(battery,descriminator,report)")
        boolean battery_warning = false ;

        @DeviceAnnotation("sensor(triggerOnChange,report)")
        String a = "low" ;

        @DeviceAnnotation("mono(a,4,triggerOnChange,report)")
        boolean b = false ;

        @DeviceAnnotation("mono(b,descriminator,report)")
        boolean c = false ; 
    }
    class DoorDetector {
        String activity = "alive" ;
    
        @DeviceAnnotation("sensor(trigger)")
        int trigger_cnt = 0 ; 

        @DeviceAnnotation("mono(trigger_cnt,20,triggerOnChange)")
        boolean trigger_cnt_mono = false ;
    }
    class HeaterDevice {

        @DeviceAnnotation("sensor()")
        float temperature = (float) 0.0 ;

        @DeviceAnnotation("actor()")
        float desired_temperature = (float) 0.0 ;
    }
    class Room {

//        DoorDetector door = new DoorDetector();
        MotionDetector motion = new MotionDetector() ;
//        HeaterDevice heater = new HeaterDevice() ;
    }
    class rooms {
 //       Room livingroom = new Room() ;
        Room hallway    = new Room();
    }
    rooms room_devices = new rooms();
    private Object devices = room_devices ;
/* ------------------------------------------------------------------ 
  ____            _      _____             _             _     _      
| __ )  __ _ ___(_) ___| ____|_ __   __ _(_)_ __   __ _| |__ | | ___ 
|  _ \ / _` / __| |/ __|  _| | '_ \ / _` | | '_ \ / _` | '_ \| |/ _ \
| |_) | (_| \__ \ | (__| |___| | | | (_| | | | | | (_| | |_) | |  __/
|____/ \__,_|___/_|\___|_____|_| |_|\__, |_|_| |_|\__,_|_.__/|_|\___|
                                    |___/                            
*/
    public void setDevices( Object dev ){
        devices = dev ;
    }

    /*
     *  INITIALIZE
     * --------------------
     */
    public void initialize() throws Exception {
        
        scanDevices( devices , _devices, "devices"  ) ;
        /*
         * Print Devices.
         */
        dumpDevices();

    }
    /*
     *     EXECUTE
     * ---------------------------------
     */
    public void execute() throws Exception {
        /*
         * Prepare Mono's
         */
        for( Mono mono : _monos.values() )mono.check() ;
        /*
         * Run the actual program.
         */
        runProgram() ;
        /*
         * Reset all
         */
        //  for( Device device : _devices.values() )device.reset() ;

    }
    void runProgram(){
        System.out.println("Running original Program!");
//        devices.hallway.heater.temperature = (float)23.5 ;
/* 
        devices.hallway.motion.motionCount_actor = devices.hallway.motion.motionCount_mono ;
        System.out.println(" runProgram : devices.hallway.motion.motionCount      : "+devices.hallway.motion.motionCount );
        System.out.println(" runProgram : devices.hallway.motion.motionCount_mono : "+devices.hallway.motion.motionCount_mono );
*/
    }
    /*
     *     SETVALUE
     * ------------------
     */
    public boolean setValue( String key , String value ) 
        throws  Exception  {

        key = "devices."+key ;
        Device device = _devices.get(key);
        if( device == null ){
            if( ! _dummyDevices.contains(key) ){
                _dummyDevices.add(key);
                throw new
                    IllegalArgumentException("Device not found (reported only once): "+key);
            }
            return false ;
        }
        return device.setValue(value);
    }
    /*
     *      GET MODIFIED ACTORS
     * --------------------------------
     */
    public Map<String,String> getModifiedActors(){
        Map<String,String> outmap = new HashMap<String,String>();
        for( Device device : _devices.values() ){
            try{
                if( device.isReport() && device.isChanged() ){
                    String name = device.getName() ;
                    if( name.startsWith("devices."))name = name.substring(8) ;
                    outmap.put( name , device.getString() ) ;
                }
                device.reset() ;
            }catch(Exception ee){

            }
        }
        return outmap;
    }
/*   
 * ---------------------------------------------------------------------------------       
 _____             _            
| ____|_ __   __ _(_)_ __   ___ 
|  _| | '_ \ / _` | | '_ \ / _ \
| |___| | | | (_| | | | | |  __/
|_____|_| |_|\__, |_|_| |_|\___|
             |___/                  
*/
    private Map<String,Device> _devices = new HashMap<String,Device>() ;
    private Map<String,Mono>   _monos   = new HashMap<String,Mono>() ;
    private Set<String>        _dummyDevices = new HashSet<String>() ;
/*
            ____             _          
            |  _ \  _____   _(_) ___ ___ 
            | | | |/ _ \ \ / / |/ __/ _ \
            | |_| |  __/\ V /| | (_|  __/
            |____/ \___| \_/ |_|\___\___|
  
 */
    private class Device {
        private String name ;
        private Set<String> set = null ;
        private Field  field ;
        private Class  clazz ;
        private Object object ;
        private String previous = "" ;
        private boolean trigger         = false ;
                boolean triggerOnChange = false ;
        private boolean report          = false ;
        private Mono    observer        = null ;


        private Device( String name ){
            this.name = name ;
        }
        public boolean setValue( String value ) throws Exception {

            this.setReflectionValue( value );

            boolean mustTrigger = false ;

            if( this.previous.equals("") ){
                mustTrigger =  false ;
            }else if( this.triggerOnChange && ! value.equals(this.previous) ){
                mustTrigger =  true ;
            }else if( this.trigger ){
                mustTrigger =  true ;
            }

            if( this.observer == null )return mustTrigger ;

            // Reminder : order ist important, first ...trigger(this) and then mustTrigger :-)

            return this.observer.trigger(this) || mustTrigger ;
        }
        private void setObserver( Mono observer ){
            this.observer = observer ;
        }
        public void setReflectionValue( String value ) throws Exception {
            if( this.clazz == int.class ){
                this.field.setInt( this.object , Integer.parseInt( value) ) ;
            }else if( this.clazz == float.class ){
                this.field.setFloat( this.object , Float.parseFloat( value) ) ;
            }else if( this.clazz == double.class ){
                this.field.setDouble( this.object , Double.parseDouble( value) ) ;
            }else if( this.clazz == long.class ){
                this.field.setBoolean( this.object , Boolean.parseBoolean( value) ) ;
            }else if( this.clazz == java.lang.String.class ){
                this.field.set( this.object , value ) ;
            }else{
                throw new 
                Exception("Unsupported internal Class of <"+this.name+"> "+this.clazz.toString());
            }
        }
        public void setBoolean( boolean value ) throws Exception {
            this.field.setBoolean( this.object , value ) ;
        }
        public String getOptionsByString(){
            StringBuffer sb = new StringBuffer() ;
            if( this.set != null ){
                for( String option : this.set )sb.append(option).append(",") ;
            }    
            return sb.toString();       
        }
        public String toString(){
            StringBuffer sb = new StringBuffer();
            sb.append("N=").append(this.name).append(";");
            sb.append("C=").append(this.clazz.getName()).append(";");
            sb.append("V=");
            try{
                sb.append(this.getString());
            }catch(Exception ee ){
                sb.append("Exception("+ee.getMessage()+")") ;
            }
            sb.append(";");
            if( this.set != null ){
                sb.append("O=[") ;
                sb.append(getOptionsByString()) ;
                sb.append("]");
            }
            return sb.toString();
        }
        public boolean isChanged() throws Exception {
            return ! this.getString().equals(this.previous);
        }
        public boolean isReport(){
            return this.report ;
        }
        public void reset() throws Exception {
            this.previous = this.getString() ;
//            System.out.println("RESETTING: "+this.getName());
        }
        public Class getValueClass(){
            return this.clazz ;
        }
        public int getInt() throws Exception {
            return this.field.getInt(this.object);
        }    
        public float getFloat() throws Exception {
            return this.field.getFloat(this.object);
        }
        public double getDouble() throws Exception {
            return this.field.getDouble(this.object);
        }
        public boolean getBoolean() throws Exception {
            return this.field.getBoolean(this.object);
        }
        public String getString() throws Exception {
            return this.field.get(this.object).toString();
        }
        public String getPreviousString(){
            return this.previous ;
        }
        private void setReflection( Field field , Object object , Class type ){
            this.field  = field ;
            this.object = object ;
            this.clazz  = type ;
        }
        void addOption(String option ){
            if( option.equals("trigger") ){
                this.trigger = true ;
            }else if( option.equals("triggerOnChange" ) ){
                this.triggerOnChange = true ;
            }else if( option.equals("report") ){
                this.report = true ;
            }else{
                if( this.set ==  null )this.set = new HashSet<String>();
                this.set.add(option);
            }
        }
        private boolean hasOption(String option){
            return this.set == null ? false : this.set.contains(option) ;
        }
        String getName(){ return this.name ; }
    }
/*
  ____                            
/ ___|  ___ _ __  ___  ___  _ __ 
\___ \ / _ \ '_ \/ __|/ _ \| '__|
 ___) |  __/ | | \__ \ (_) | |   
|____/ \___|_| |_|___/\___/|_|   
 
                                                
*/
    private class Sensor extends Device {
        private Sensor( String name ){
            super( name ) ;
        }
        public String toString(){
            StringBuffer sb = new StringBuffer() ;
            sb.append("Sensor(") ;
            return sb.toString() ;
        }
    }
/*
    _        _             
   / \   ___| |_ ___  _ __ 
  / _ \ / __| __/ _ \| '__|
 / ___ \ (__| || (_) | |   
/_/   \_\___|\__\___/|_|   
 */
    private class Actor extends Device {
        private Actor( String name ){
            super( name ) ;
            addOption("report");
        }
        public String toString(){
            return "Actor();"+super.toString(); 
        }
    }
/*
 __  __                   
|  \/  | ___  _ __   ___  
| |\/| |/ _ \| '_ \ / _ \ 
| |  | | (_) | | | | (_) |
|_|  |_|\___/|_| |_|\___/ 
                    
 */
    private class Mono extends Device {
        private String target     = null ;
        private long   delay      = 0L ;
        private long   started    = 0L ;
        private boolean retrigger = true ;
        private boolean triggerOnlyOnSourceChanged = true ;
        private Mono( String name ){
            super( name ) ;
        }
        public boolean setValue( String value ) throws Exception {
            throw new
            IllegalArgumentException("Can't set Mono value!");
        }
        private void setTarget( String target ){
            this.target = target ;
        }
        private String getTarget(){
            return this.target ;
        }
        private void setDelay( long delay ){
            this.delay = delay * 1000L ;
        }        
        private void setDescriminator(){
            this.delay = -1 ;
        }
        public void addOption( String option ) {
            if( option.equals("noRetrigger") ){
                this.retrigger = false ;
            }else if( option.equals( "triggerOnSourceRepeats" ) ){
                this.triggerOnlyOnSourceChanged = false;
            }else{
                super.addOption(option);
            }
        }
        private boolean trigger( Device source ) throws Exception {
            if( delay < 0 ){
                /*
                 * Decriminator
                 */
                if( ( ! this.triggerOnlyOnSourceChanged ) || source.isChanged() ) {
                    super.setBoolean(true);
                }
            }else{
                /*
                 * Monoflop
                 */
                if( ( ( ! this.triggerOnlyOnSourceChanged ) || source.isChanged() ) &&
                    ( ( ! this.check() ) || this.retrigger     )    ) {
                        this.started = System.currentTimeMillis() ;
                        super.setBoolean(true);
                }
            }
            boolean shouldTrigger = this.isChanged() || ! this.triggerOnChange ;

            if( super.observer != null )shouldTrigger = super.observer.trigger(this) | shouldTrigger ;

            return shouldTrigger ;
        }
        public String toString(){
            return "Mono("+this.target+","+delay+");"+super.toString(); 
        }
        public void reset() throws Exception {
            if( delay < 0 )super.setBoolean(false);
            super.reset() ;
        }
        public boolean check() throws Exception {
            if( this.delay < 0 ){
                return false ;
            }else{
                boolean isTriggered = ( System.currentTimeMillis() - this.started ) < delay  ;
                super.setBoolean(isTriggered) ;
                return isTriggered ;
            }
        }
    }
    /*
     * 
     */
    public Device getDevice( String key ) throws Exception {
        Device device = _devices.get(key);
        if( device == null )throw new IllegalArgumentException("No such device : "+key);
        return device ;
    }
    public void resetDevices(){
        for( Device device : _devices.values() ){
            try{
                device.reset();
            }catch(Exception ee ){

            }
        }
    }
    private void  scanDevices( Object obj , Map<String,Device> map , String prefix) 
        throws Exception {
        walkObject( obj , map , prefix ) ;
        for( Device device : map.values() ){
            if( device instanceof Mono ){
                Mono mono = (Mono)device ;
                String targetName = mono.getTarget() ;
                Device target = map.get(targetName) ;
                if( target == null )
                    throw new
                    IllegalArgumentException(
                    "Program error: target("+targetName+
                    ") of Mono "+device.getName()+" not found!");

                target.setObserver(mono);

                _monos.put( device.getName() , (Mono) device ) ;
            }
        }
    }
    private void  walkObject( Object obj , Map<String,Device> map , String prefix) throws Exception {
        Class cls = obj.getClass();
        System.out.println("Walking starts at "+prefix+" ["+cls.getName()+"]" );
        
        Field fieldlist[] = cls.getDeclaredFields() ; 
        for( int i = 0 ; i < fieldlist.length ; i++ ){

            Field field  = fieldlist[i];
            if( field.getName().equals("this$0") )continue ;
            String name  = prefix+"."+field.getName();
            Class type   = field.getType();
            boolean p    = type.isPrimitive() ;
            
            System.out.println("Found : "+field.getName()+" ["+type.getName()+"]");

            DeviceAnnotation dd = field.getAnnotation(DeviceAnnotation.class);
            String annotations  = dd == null ? "" : dd.value();

            if( p  || ( type == java.lang.String.class ) ){
                    Device device = createDevice( name , field , obj, type, annotations );
                    map.put( name , device );
            }else{
                walkObject( field.get(obj) , map , name );
            }
        }
    }
    public List<String> parseAnnotation(String annotation ) throws IllegalArgumentException {

        List<String> list = new ArrayList<String>() ;
        String  pattern   = 
        "^(global|sensor|actor|mono)\\(((?:[a-zA-Z0-9_]"+
        "[a-zA-Z0-9_\\.]*)(?:,[a-zA-Z0-9_][a-zA-Z0-9_\\.]*)*)?\\)$";
        Pattern regex     = Pattern.compile(pattern);
        Matcher matcher   = regex.matcher(annotation);

        if (matcher.matches()) {
            String function = matcher.group(1);           // e.g., sensor
            list.add(function);

            String argList  = matcher.group(2);            // e.g., "a,b,c,d4"

            if (argList != null && !argList.isEmpty()) {
                String[] argsSplit = argList.split(",");
                for (int i = 0; i < argsSplit.length; i++)list.add(argsSplit[i]) ;
            }
        } else {
            throw new
            IllegalArgumentException("Syntax error in Annotation : "+annotation);
        }
        return list ;
    }

    private Device createDevice( String name , 
                                 Field field , 
                                 Object obj , 
                                 Class type , 
                                 String annotations )
                throws IllegalArgumentException {
        /*
         * Syntax :
         *     sensor(trigger|onChange)
         *     actor
         *     mono(<target>,<delay>|'desc'[,onChange])
         */
        annotations = annotations.strip();
        if( annotations.length() ==0 ){
            /*
             * Just a global variable.
             */
            Device device = new Device(name);
            device.setReflection( field , obj , type ) ;
            return device ;
        }
        List<String> list = parseAnnotation( annotations ) ;
        Device device = null ;
        int    cursor = 0 ;
        int    max    = list.size() ;

        String func   = list.get(cursor++);

        if( func.equals("actor" ) ){

            device = new Actor( name ) ;

        }else if( func.equals("mono" ) ){
            if( cursor >= max)
                throw new 
                IllegalArgumentException("mono of <"+name+"> expects > 0, found 0!") ;

            Mono mono = new Mono( name ) ;
            /*
             * Argument 1 : target (relative to this class or absolute with '.'s)
             */
            String targetName = list.get(cursor++) ;
            if( targetName.indexOf(".") == -1 ){
                int dot = name.lastIndexOf(".") + 1 ;
                targetName = name.substring(0,dot) + targetName ;
            }
            mono.setTarget( targetName ) ;
            /*
             * Argument 2 : delay eiter number or 'desc[riminator]' for delay = -1 ;
             */
            if( cursor < max ){
                String delayString = list.get(cursor++) ;
                int delay = delayString.startsWith("desc") ? -1 : Integer.parseInt( delayString ) ;
                mono.setDelay( delay ) ;
            }

            device = mono ;
                
        }else if( func.equals("sensor" ) ){
            device = new Sensor(name) ;
        }
        /*
         * use add option to add the remaning options. Some options are cought by
         * the implentions of the superclasses of 'Device'. The rest is just stored
         * in 'Device' options.
         */
        while( cursor < max )device.addOption(list.get(cursor++)) ;             
        
        device.setReflection( field , obj , type ) ;

        return device ;
    }

    private void dumpDevices(){
        System.out.println("\n ------ Listing devices!\n");
        //for( Device device : _devices.values() ){
        //    System.out.println(device.toString());
        //}
        List<String> sortedNames = new ArrayList<>(_devices.keySet()) ;
        Collections.sort(sortedNames);
        int maxNameLength = 0 ;
        for( String name : sortedNames )
            maxNameLength = Math.max(name.length(),maxNameLength);
        int maxValueLength    = 0 ;
        int maxPreviousLength = 0 ;
        for( Device device : _devices.values() )
            try{
                maxValueLength = Math.max(device.getString().length() , maxValueLength ) ;
                maxPreviousLength = Math.max(device.getPreviousString().length(),maxPreviousLength);
            }catch(Exception ee ){}

        for( String name : sortedNames ){
            Device device = _devices.get(name);
            StringBuffer sb = new StringBuffer() ;

            int n = maxNameLength - name.length() ;
            sb.append(name).append(" ").append(".".repeat(n)).append(" ") ;
            if( device instanceof Sensor ){
                sb.append("[S");
            }else if( device instanceof Actor ){
                sb.append("[A");
            }else if( device instanceof Mono ){
                sb.append("[M");
            }else{
                sb.append("[D");
            }
            /*
             *  THE CLASS
             */
            String className = device.getValueClass().getName().substring(0,1).toUpperCase() ;
            sb.append(className.equals("J") ? "S" : className);
            /*
             * should report (actor or 'report' annotation)
             */
            sb.append( device.report ? "R" : "r" ) ;
            sb.append( device.trigger ? "T" : "t" ) ;
            sb.append( device.triggerOnChange ? "X" : "x" ) ;
            if( device instanceof Mono ){
                Mono mono = (Mono)device ;
                sb.append( mono.retrigger ? "G" : "g" ) ;
                sb.append( mono.triggerOnlyOnSourceChanged ? "O" : "o" ) ;
            }else{
                sb.append("--");
            }
            /*
             * was changed
             */
            try{
                sb.append(device.isChanged()?"C":".") ;
            }catch(Exception ee){
                sb.append("X") ;
            }
            sb.append("] ");
            /*
             * VALUES [ previous values ]
             */
            try{
                String value = device.getString() ;
                n = value.length() ;
                sb.append(" ".repeat(maxValueLength-n)).append(value).append(" ");
            }catch(Exception ee){
                sb.append("".repeat(maxValueLength-5)).append("error ");
            }
            String value = device.getPreviousString() ;
            n = value.length() ;
            sb.append( " [").append(" ".repeat(maxPreviousLength-n)).append(value).append("] ");

            if( device instanceof Mono ){
                Mono mono = (Mono)device ;
                sb.append( "{").append(mono.getTarget()).append("} ");
            }
                
            if( device.observer != null )
                    sb.append( "{").append(device.observer.getName()).append("} ");
            
            sb.append("(").append(device.getOptionsByString()).append(")");
            System.out.println(sb.toString());
        }
    }
    public SimpleJavaEngine( ModuleContext context ){

      //  new Thread(this,"executing").start() ;
      //  new Thread(this,"ingest").start() ;
      //  new Thread(this,"reporting").start() ;

    }
    public void run()  {
        Thread current = Thread.currentThread() ;
        if( "executing".equals(current.getName())){
            run_execution() ;
        }else if("ingest".equals(current.getName())){
            run_ingest() ;
        }else if("reporting".equals(current.getName())){
            run_reporting() ;
        }
    }
    private void run_execution(){

    }
    private void run_ingest(){

    }
    private void run_reporting(){

    }
    public static void main( String [] args ) throws Exception {

        Map<String,String> contextMap = new HashMap<String,String>();
        ModuleContext      context    = new ModuleContext( "engine" , contextMap );
        SimpleJavaEngine   engine     = new SimpleJavaEngine( context ) ;

        engine.initialize();
        engine.execute() ;
        engine.dumpDevices() ;

        for( int i = 0 ; i < 1000000 ; i++){

            Thread.sleep(1000);
            System.out.println("\n ******* Count "+i+" ---------------------------\n");
            boolean trigger = false ;

            if( i == 2 ){
                trigger = engine.setValue("devices.hallway.motion.battery","low");
            }
            if( ( i >  3 ) && ( i < 10 ) ){
                trigger = engine.setValue("devices.hallway.motion.motionCount","count_3_"+i);
            }
            if( i == 3 )trigger = engine.setValue("devices.hallway.motion.a" , "high");

            if( trigger ){
                System.out.println("   ------- EXECUTE : ON REQUEST");
                engine.execute() ;
            }else if( i % 5 == 0 ){
                System.out.println("   ------- EXECUTE : Tick Tack");
                engine.execute() ;
            }


            engine.dumpDevices() ;

            System.out.println("   ------- Dumping changed actors: ");
            Map<String,String> changedActors = engine.getModifiedActors() ;
            for( Map.Entry<String,String> e : changedActors.entrySet() ){
                System.out.println(e.getKey()+" -> "+e.getValue() ) ;
            }
            System.out.println("   --------------------------------");

        }
//      engine.getDevice("devices.hallway.heater.temperature").getFloat();
    }
}
