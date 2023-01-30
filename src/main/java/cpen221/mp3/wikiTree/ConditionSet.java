package cpen221.mp3.wikiTree;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConditionSet {

    //ConditionSet

    //Abstraction Function
    /*
    Represents a pair of conditions that indicate if a Node can continue to make children nodes in a buildTree
    operation. The first condition is that the current time is not greater than the expiry time of the object.
    The expiry time, which is in nanoseconds since the system timer started, is stored in the private final long
    "expiry". The timespan that a ConditionSet can be valid for is specified by the user, therefore "expiry" is
    characterized as the creation time of the ConditionSet in relation to the system timer + the user specified
    timeframe.

    The second condition is that the destination object has not been found yet by another node. It is stored in the
    private boolean "found" and is initially false by definition, but can be updated to true using the terminate method.
    Once it has been called "true" it is effectively final, as there is no way to further mutate the object.

    A ConditionSet is meant to be a global object by its mechanics.
     */

    //Representation Invariant
    /*
    1. found may not be true if the destination node has not been found within the Node tree.
    2. The expiry time must be a logical value within the context of the system timer.
     */

    //Thread Safety Arguments
    /*
    Because the expiry field of a ConditionSet is immutable, and the check method is only an observer method, the only
    method/fields of concern are "found" and "terminate". To safeguard against errors in multi-threaded environments,
    leading to terminate modifying "found" unpredictably, the method was made to be synchronized.
     */

    private boolean found;
    private final long expiry;

    //Due to not being able to access specific nodes from this class, and since there is no concrete way to determine
    //the validity of the expiry time, no checkRep is provided for this class.

    /**
     * Constructor for a ConditionSet
     * @param expiry    the time that can elapse between a ConditionSet's creation to when it becomes invalid, in
     *                  seconds.
     */
    public ConditionSet(long expiry){
        this.found = false;
        long initializationTime = System.nanoTime();
        this.expiry = TimeUnit.NANOSECONDS.convert(expiry, TimeUnit.SECONDS) + initializationTime;
    }

    /**
     * Checks to see if the current instance of ConditionSet is valid within the scope of the conditions (as laid out
     * in the abstraction function).
     * @return  true, if the conditions are still valid, false if one or more of the conditions are not valid
     * @throws TimeoutException if the ConditionSet has "timed out"
     */
    public boolean check() throws TimeoutException {
        if(System.nanoTime() > this.expiry){
            throw new TimeoutException();
        }
        return (!(this.found));
    }

    /**
     * Used to switch the "found" boolean to true once the destination object has been found. Modifies the "found"
     * boolean to true.
     */
    public synchronized void terminate(){
        this.found = true;
    }
}
