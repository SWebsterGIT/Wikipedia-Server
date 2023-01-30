package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.testing.TestT;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class Task1Tests {

    @Test
    public void noTimeouts() {
        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>();
        Set<TestT> testList = new HashSet<>(List.of(alpha, bravo, charlie));

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        Set<TestT> checkSet = new HashSet<>(testBuffer.getCurrentObjects());


        Assert.assertEquals(testList, checkSet);
    }

    @Test
    public void allTimeout() {
        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(1000, 1);
        Set<TestT> emptyList = new HashSet<>();

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(emptyList, testBuffer.getCurrentObjects());
    }

    @Test
    public void overflow() {
        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(2, 1);
        Set<TestT> expected = new HashSet<>(List.of(charlie, alpha));

        testBuffer.put(alpha);
        testBuffer.put(bravo);

        testBuffer.get("alpha");

        testBuffer.put(charlie);

        Assert.assertEquals(expected, testBuffer.getCurrentObjects());
    }

    @Test
    public void notAllExpired() {
        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");
        TestT echo = new TestT("echo");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(3, 1);
        Set<TestT> expected = new HashSet<>(List.of(alpha, delta, charlie));

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(echo);
        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testBuffer.put(charlie);
        testBuffer.put(delta);
        testBuffer.put(alpha);


        Assert.assertEquals(expected, testBuffer.getCurrentObjects());
    }

    @Test
    public void touchedObject() {

        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");
        TestT echo = new TestT("echo");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(10, 2);

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testBuffer.touch("bravo");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        testBuffer.put(delta);
        testBuffer.put(echo);

        Assert.assertEquals(bravo, testBuffer.get("bravo"));
        Assert.assertEquals(delta, testBuffer.get("delta"));
        Assert.assertEquals(echo, testBuffer.get("echo"));

    }

    @Test
    public void updateObject() {

        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");
        TestT echo = new TestT("echo");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(10, 2);
        Set<TestT> expected = new HashSet<>(List.of(bravo, delta, echo));

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testBuffer.update(bravo);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testBuffer.put(delta);
        testBuffer.put(echo);

        Assert.assertEquals(expected, testBuffer.getCurrentObjects());
    }

    @Test
    public void noUpdate() {

        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");
        TestT echo = new TestT("echo");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(10, 2);
        Set<TestT> expected = new HashSet<>(List.of(delta, echo));

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testBuffer.put(delta);
        testBuffer.put(echo);

        Assert.assertEquals(expected, testBuffer.getCurrentObjects());
    }

    @Test
    public void falseTouch() {
        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>();

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        Assert.assertFalse(testBuffer.touch("delta"));
    }

    @Test
    public void falseUpdate() {
        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>();

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        Assert.assertFalse(testBuffer.update(delta));
    }

    @Test
    public void falsePutTimeout() {
        TestT alpha = new TestT("alpha");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(0, 1);

        Assert.assertFalse(testBuffer.put(alpha));
        Assert.assertEquals(0, testBuffer.size());
    }

    @Test
    public void falsePutCapacity() {
        TestT alpha = new TestT("alpha");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(1, 0);

        Assert.assertFalse(testBuffer.put(alpha));
        Assert.assertEquals(0, testBuffer.size());
    }

    @Test
    public void putToUpdate() {

        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");
        TestT charlie = new TestT("charlie");
        TestT delta = new TestT("delta");
        TestT echo = new TestT("echo");

        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>(10, 2);
        Set<TestT> expected = new HashSet<>(List.of(bravo, delta, echo));

        testBuffer.put(alpha);
        testBuffer.put(bravo);
        testBuffer.put(charlie);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testBuffer.put(bravo);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testBuffer.put(delta);
        testBuffer.put(echo);

        Assert.assertEquals(expected, testBuffer.getCurrentObjects());
    }

    @Test(expected = NoSuchElementException.class)
    public void noElement() {
        FSFTBuffer<TestT> testBuffer = new FSFTBuffer<>();

        TestT alpha = new TestT("alpha");
        TestT bravo = new TestT("bravo");

        testBuffer.put(alpha);

        testBuffer.get("bravo");
    }


}
