package cpen221.mp3.testing;

import cpen221.mp3.fsftbuffer.FSFTBuffer;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ConcurrentBufferTester extends Thread {
    private final CyclicBarrier barrier;
    private final FSFTBuffer<TestT> buffer;
    private final String task;
    private final String taskId;

    /**
     * Create one threaded call for a buffer test. This class is intented
     * for simultaneous thread testing on an FSFTBuffer.
     * @param buffer FSFTBuffer that is being tested
     * @param barrier to start thread at "exact" same times
     * @param task to perform on the buffer
     * @param taskId name of the task
     */
    public ConcurrentBufferTester(FSFTBuffer<TestT> buffer, CyclicBarrier barrier,
                                  String task,
                                  String taskId) {
        this.barrier = barrier;
        this.buffer = buffer;
        this.task = task;
        this.taskId = taskId;
    }


    @Override
    public void run() {
        try {
            barrier.await();
            if ("put".equals(task)) {
                buffer.put(new TestT(taskId));
            } else if ("get".equals(task)) {
                buffer.get(taskId).id();
            } else if ("touch".equals(task)) {
                buffer.touch(taskId);
            } else if ("update".equals(task)) {
                buffer.update(new TestT(taskId));
            }
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }


    }
}
