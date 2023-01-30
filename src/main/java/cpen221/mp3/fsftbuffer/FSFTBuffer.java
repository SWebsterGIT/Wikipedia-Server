package cpen221.mp3.fsftbuffer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FSFTBuffer<T extends Bufferable> {

    //FSFTBuffer

    //Abstraction Function
    /*
    The FSFTBuffer is a form of a finite-time and finite-space buffer. It uses a HashMap called "buffer" which stores
    a Bufferable object, which is any object that has a String field that give it a unique id. Each entry into the
    buffer map is a key-value pair where the key is the Bufferable object, and the value is a TimePair. The TimePair
    stores the access time and the expiry time of its keyed object. More details on the TimePair class can be found
    in the TimePair abstraction function in the TimePair class. Aside from the HashMap called "buffer", the FSFTBuffer
    also has two other fields, an integer "capacity" which is the FSFTBuffer's maximum number of objects that it can
    store, and another integer "timeout" which is the number of seconds that an object can remain in the buffer without
    that object being refreshed. There also exist two static fields, called "DSIZE" and "DTIMEOUT" which respectivley
    are default values for the capacity and timeout of the buffer. They are used when a new FSFTBuffer is declared
    without specified capacity or size.
     */

    //Representation Invariants
    /*
    An FSFTBuffer has multiple representation invariants.

    1. buffer.size() may never exceed capacity.
    2. No object within the buffer map may have an expiry time larger greater than the current time of the system.
    3. All objects stored within the buffer must be not-null and extend Bufferable.
    4. Each Bufferable object stored in the FSFTBuffer as a key must have a corresponding TimePair as a value.
     */

    //Thread Safety Arguments

    /*
    This class is thread safe for two main reasons.

    Immutability -  The fields DSIZE, DTIMEOUT, capacity, and timeout are all final, and capacity and timeout are also
                    private.

    Synchronization -   Although the contents of the HashMap buffer are mutated by the various methods within
                        an FSFTBuffer, each method which may mutate the contents of the FSFTBuffer are synchronized
                        methods, thus safeguarding against errors arising due to multithreading.
     */

    /**
     * checkRep method for an FSFTBuffer. Throws a RuntimeException if the rep has been violated during the call to
     * checkRep.
     */
    private synchronized void checkRep(){
        boolean rep = true;
        if(buffer.size() > capacity){
            rep = false;
        }
        long time = System.nanoTime();

        if(buffer.values().stream().filter(x -> x.getExpiry() + timeout < time).count() != this.size()){
            rep = false;
        }

        if(buffer.keySet().stream().filter(Objects::nonNull).count() != this.size()){
            rep = false;
        }

        if(buffer.values().stream().filter(Objects::nonNull).count() != this.size()){
            rep = false;
        }

        assert(rep);

    }

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private final int capacity;
    private final long timeout;

    private final HashMap<T, TimePair> buffer = new HashMap<>();

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.capacity = capacity;
        this.timeout = TimeUnit.NANOSECONDS.convert(timeout, TimeUnit.SECONDS);
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object. If the object that is attempting to be added
     * is already in the buffer, refresh ONLY the TIMEOUT TIME of that object, not the access time.
     *
     * @return true, if the buffer has valid timeout and timeout times, false if not.
     */
    public synchronized boolean put(T t) {

        //checks if the buffer has valid values
        if(this.timeout <= 0 || this.capacity <= 0){
            return false;
        }

        if(this.buffer.containsKey(t)){
            this.update(t);
        }

        this.clean();

        //Bumps out least recently accessed objects
        while(this.buffer.size() >= capacity){
            this.buffer.remove(this.buffer.entrySet()
                .stream()
                .min((a, b) -> a
                    .getValue()
                    .getAccess() > b.getValue().getAccess()
                    ? 1 : -1).get().getKey());
        }

        this.buffer.put(t, new TimePair());

        return true;
    }

    /**
     * Retrives an object from the buffer based on its id, throws a NoSuchElementException if the object is not found.
     * @param id    The id of the desired object from the buffer
     * @return      The desired object inside the buffer.
     * @throws NoSuchElementException   if no Bufferable inside the buffer has the id passed as the parameter in the
     *                                  method.
     */
    public synchronized T get(String id) throws NoSuchElementException {
        this.clean();

        List<T> matchSet = this.buffer.keySet().stream().filter(x -> x.id().equals(id)).collect(Collectors.toList());

        if(matchSet.size() > 0){
            this.buffer.replace(matchSet.get(0), this.buffer.get(matchSet.get(0)).updateAccessTime());
            return matchSet.get(0);
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise, where a false return
     * would result from the object not being
     * currently stored in the buffer.
     */
    public synchronized boolean touch(String id) {

        this.clean();

        //Identifies keys matching the id in the buffer.
        List<T> matchSet = this.buffer.keySet().stream().filter(x -> x.id().equals(id)).collect(Collectors.toList());

        if(matchSet.size() > 0){
            this.buffer.replace(matchSet.get(0), this.buffer.get(matchSet.get(0)).updateGenerationTime());
            return true;
        } else {
            return false;
        }

    }

    /**
     * Update an object in the buffer.
     * This method updates an object and acts like a "touch" to
     * renew the object in the cache.
     *
     * @param t the object to update
     * @return true if the object is within the buffer and false otherwise
     */
    public synchronized boolean update(T t) {

        this.clean();

        if(this.buffer.containsKey(t)){
            this.buffer.replace(t, this.buffer.get(t).updateGenerationTime());
            return true;
        } else {
            return false;
        }

    }

    /**
     * Helper method for all other methods within the class. clean is called before every method executes in the
     * FSFTBuffer. It removes all objects within the buffer which have expired, where "expiry" is defined as if
     * the object's TimePair expiry time is lesser than the current System.nanoTime().
     */
    private synchronized void clean(){
        long cleanTime = System.nanoTime();

        if(this.buffer.size() > 0){
            List<T> expiredKeys = this.buffer.keySet().stream()
                    .filter(x -> this.buffer.get(x).getExpiry() + timeout < cleanTime).collect(Collectors.toList());

            for(T object : expiredKeys){
                this.buffer.remove(object);
            }
        }

    }

    /**
     * Testing method for the FSFTBuffer. Returns the set of all of the current objects within the buffer.
     * Requires that the buffer has at least one object within it.
     * @return  The set of all of the current objects currently stored inside of the buffer.
     */
    public synchronized Set<T> getCurrentObjects() {
        this.clean();
        return new HashSet<>(this.buffer.keySet());
    }

    /**
     * Retrieves the current size of the FSFTBuffer.
     * @return  The current size of the current instance of FSFTBuffer.
     */
    public synchronized int size(){
        this.clean();
        return this.buffer.size();
    }


}
