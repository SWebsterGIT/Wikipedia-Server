package cpen221.mp3.testing;

import cpen221.mp3.fsftbuffer.Bufferable;
import cpen221.mp3.fsftbuffer.FSFTBuffer;

public class ConcurrentPut<T extends Bufferable> implements Runnable {

    private final FSFTBuffer<T> buffer;
    private final T object;

    public ConcurrentPut(FSFTBuffer<T> buffer, T object){
        this.buffer = buffer;
        this.object = object;
    }

    @Override
    public void run() {
        this.buffer.put(this.object);
    }
}
