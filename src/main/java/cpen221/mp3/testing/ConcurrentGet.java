package cpen221.mp3.testing;

import cpen221.mp3.fsftbuffer.Bufferable;
import cpen221.mp3.fsftbuffer.FSFTBuffer;

public class ConcurrentGet<T extends Bufferable> implements Runnable {

    private final FSFTBuffer<T> buffer;
    private final String id;
    private TestT result;

    public ConcurrentGet(FSFTBuffer<T> buffer, String id){
        this.buffer = buffer;
        this.id = id;
    }

    @Override
    public void run() {
        result = (TestT) this.buffer.get(this.id);
    }

    public TestT execute(){
        this.run();
        return this.result;
    }
}
