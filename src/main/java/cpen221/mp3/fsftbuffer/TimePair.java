package cpen221.mp3.fsftbuffer;

import java.sql.Time;

public class TimePair {

    //TimePair

    //Abstraction Function
    /*
    A TimePair is a helper class for an FSFTBuffer. It stores two relevant times to a buffer, an object's last access
    time and an object's "last" "generation" time, both in nanoseconds since the start of Java's system timer, which is
    an arbitrary value. The last access time is stored in the private final long "accessTime", and the object's last
    generation time is stored in the private final long "generationTime ". Because the fields of this
    object are both private, final, and primitive, a TimePair is naturally immutable. A generation time, in the context
    of an FSFTBuffer is the time that an object was first placed in the buffer, or the last time that the object was
    touched or update, and an access time is the last time an object was placed, or accessed within the buffer via get.
     */

    //Representation Invariants
    /*
    Both generationTime and accessTime are not null.
     */

    //Thread Safety Arguments
    /*
    Immutability - TimePair is an immutable type, therefore it is thread-safe.
     */

    private final long generationTime;
    private final long accessTime;

    //Due to the final and immutable nature of the fields within TimePair, there is no reasonable checkRep function
    // that can be created.

    /**
     * Default constructor for a new TimePair, where the generationTime and accessTimes are the current time.
     */
    public TimePair(){
        this.generationTime = System.nanoTime();
        this.accessTime = System.nanoTime();
    }

    /**
     * Secondary constructor for a new TimePair, where the generationTime and accessTimes are inputted by the user.
     * Requires that the times are in nanoseconds and are valid within the scope of the current system timer.
     * @param generationTime    the desired generationTime to pass to the new TimePair object.
     * @param accessTime        the desired accessTime to pass to the new TimePair object.
     */
    public TimePair(long generationTime, long accessTime){
        this.generationTime = generationTime;
        this.accessTime = accessTime;
    }

    /**
     * Creates a new TimePair from an existing TimePair, where the generationTime is updated to the current system time,
     * and the accessTime is the accessTime of the existing TimePair
     * @return  a clone of the original TimePair with an updated generationTime.
     */
    public TimePair updateGenerationTime(){
        return new TimePair(System.nanoTime(), this.accessTime);
    }

    /**
     * Creates a new TimePair from an existing TimePair, where the generationTime is the generationTime of the existing,
     * timePair, and the accessTime is the current system time.
     * @return  a clone of the original TimePair with an updated accessTime.
     */
    public TimePair updateAccessTime(){
        return new TimePair(this.generationTime, System.nanoTime());
    }


    /**
     * Returns the accessTime of the current instance of TimePair
     * @return  the accessTime of the current instance of TimePair
     */
    public long getAccess(){
        return this.accessTime;
    }

    /**
     * Returns the generationTime (called expiry for ease of use in FSFTBuffer, although not a true "expiry") of the
     * current instance of TimePair
     * @return  the time that the current instance of TimePair was generated.
     */
    public long getExpiry(){
        return this.generationTime;
    }

}
