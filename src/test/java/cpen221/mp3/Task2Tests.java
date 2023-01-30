package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.testing.ConcurrentBufferTester;
import cpen221.mp3.testing.ConcurrentGet;
import cpen221.mp3.testing.ConcurrentPut;
import cpen221.mp3.testing.TestT;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class Task2Tests {

    @Test
    public void concurrentPut() throws InterruptedException {

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>();

        TestT alpha = new TestT("alpha");

        ConcurrentPut test = new ConcurrentPut(testBuffer, alpha);

        Thread thread = new Thread(test);

        thread.start();

        thread.join();

        Assert.assertEquals(alpha, testBuffer.get("alpha"));
    }

    @Test
    public void multiplePuts() throws InterruptedException {
        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>();

        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");
        TestT echo = new TestT("echo");
        TestT foxtrot = new TestT("foxtrot");

        ConcurrentPut aPut = new ConcurrentPut(testBuffer, alpha);
        ConcurrentPut bPut = new ConcurrentPut(testBuffer, bravo);
        ConcurrentPut cPut = new ConcurrentPut(testBuffer, charlie);
        ConcurrentPut dPut = new ConcurrentPut(testBuffer, delta);
        ConcurrentPut ePut = new ConcurrentPut(testBuffer, echo);
        ConcurrentPut fPut = new ConcurrentPut(testBuffer, foxtrot);

        List<ConcurrentPut> tList = new ArrayList<>(List.of(aPut, bPut, cPut, dPut, ePut, fPut));

        List<Thread> threadList = new ArrayList<>();

        for(ConcurrentPut runnable : tList){
            Thread thread = new Thread(runnable);
            threadList.add(thread);
            thread.start();
        }

        for(Thread thread : threadList){
            thread.join();
        }


        Set<TestT> expectedSet = new HashSet<>(Set.of(alpha, bravo, charlie, delta, echo, foxtrot));



        Assert.assertEquals(expectedSet, testBuffer.getCurrentObjects());
    }

    @Test
    public void get(){
        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>();

        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");
        TestT echo = new TestT("echo");
        TestT foxtrot = new TestT("foxtrot");

        ConcurrentPut aPut = new ConcurrentPut(testBuffer, alpha);
        ConcurrentPut bPut = new ConcurrentPut(testBuffer, bravo);
        ConcurrentPut cPut = new ConcurrentPut(testBuffer, charlie);
        ConcurrentPut dPut = new ConcurrentPut(testBuffer, delta);
        ConcurrentPut ePut = new ConcurrentPut(testBuffer, echo);
        ConcurrentPut fPut = new ConcurrentPut(testBuffer, foxtrot);

        aPut.run();
        bPut.run();
        cPut.run();
        dPut.run();
        ePut.run();
        fPut.run();

        ConcurrentGet aGet = new ConcurrentGet(testBuffer, "alpha");



        TestT result = aGet.execute();

        Assert.assertEquals(alpha, result);
    }


    @Test
    public void sameTimePuts() throws InterruptedException, BrokenBarrierException {
        final CyclicBarrier barrier = new CyclicBarrier(3);
        FSFTBuffer<TestT> buffer = new FSFTBuffer<>();

        ConcurrentBufferTester put1 = new ConcurrentBufferTester(
            buffer, barrier, "put", "put1"
        );
        ConcurrentBufferTester put2 = new ConcurrentBufferTester(
            buffer, barrier, "put", "put2"
        );

        put1.start();
        put2.start();
        barrier.await();

        put1.join();
        put2.join();
        Assert.assertEquals(2, buffer.size());
    }

    @Test
    public void fullCacheSize() throws BrokenBarrierException, InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        FSFTBuffer<TestT> buffer = new FSFTBuffer<>(1, 30);
        buffer.put(new TestT("initial"));

        ConcurrentBufferTester put1 = new ConcurrentBufferTester(
            buffer, barrier, "put", "put1"
        );

        final int[] buffSize = new int[1];
        Thread size = new Thread(new Runnable() {
            @Override
            public void run() {
                buffSize[0] = buffer.size();
            }
        });

        put1.start();
        size.start();
        barrier.await();

        put1.join();
        size.join();
        Assert.assertEquals(1, buffSize[0]);
    }

    @Test
    public void getAndRemoved() throws InterruptedException {
        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(2, 30);

        TestT A = new TestT("alpha");
        TestT B = new TestT("bravo");
        TestT C = new TestT("charlie");


        Thread putA = new Thread(() -> {
            testBuffer.put(A);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testBuffer.get(A.id());
        });

        Thread putB = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testBuffer.put(B);
        });

        Thread putC = new Thread(() -> {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testBuffer.put(C);
        });

        putA.start();
        putB.start();
        putC.start();

        putA.join();
        putB.join();
        putC.join();

        testBuffer.getCurrentObjects().forEach(x -> System.out.println(x.id()));
        Assert.assertEquals(Set.of(A,C), testBuffer.getCurrentObjects());
    }

    @Test
    public void touchAndExpire() throws InterruptedException {
        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(2, 3);

        TestT A = new TestT("alpha");
        TestT B = new TestT("bravo");

        Thread putA = new Thread(() -> {
            testBuffer.put(A);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testBuffer.touch(A.getValue());
        });

        Thread putB = new Thread(() -> {
            testBuffer.put(B);
            try {
                Thread.sleep(3300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


        putA.start();
        putB.start();
        putA.join();
        putB.join();



        Assert.assertEquals(Set.of(A), testBuffer.getCurrentObjects());
    }

    @Test
    public void updateAndExpire() throws InterruptedException {
        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(2, 3);

        TestT A = new TestT("alpha");
        TestT B = new TestT("bravo");

        Thread putA = new Thread(() -> {
            testBuffer.put(A);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testBuffer.update(A);
        });

        Thread putB = new Thread(() -> {
            testBuffer.put(B);
            try {
                Thread.sleep(3300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


        putA.start();
        putB.start();
        putA.join();
        putB.join();

        Assert.assertEquals(Set.of(A), testBuffer.getCurrentObjects());
    }

}
