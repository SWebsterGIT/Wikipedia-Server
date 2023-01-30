package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.fastily.jwiki.core.Wiki;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;


public class Task3Tests {
    // TODO: Come up with and test edge cases

    @Test
    public void searchTest() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals(Arrays.asList("Barack Obama", "Talk:Obama", "Talk:OBAMA"),
            mediator.search("Obama", 3));
    }

    @Test
    public void searchTest_Empty() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals(List.of(), mediator.search("", 5));
    }

    @Test
    public void searchTest_noResults() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals(List.of(), mediator.search("akjshdfkjahsdouifhjweojncuh", 5));
    }

    @Test
    public void searchTest_null() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals(List.of(), mediator.search(null, 5));
    }

    @Test
    public void searchTest_space() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals(List.of(), mediator.search(" ", 5));
    }

    @Test
    public void searchTest_zeroLimit() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals(List.of(), mediator.search("Dog", 0));
    }

    @Test
    public void pageTest() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        Assert.assertEquals(wiki.getPageText("Barack Obama"), mediator.getPage("Barack Obama"));
    }

    @Test
    public void pageTest_empty() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals("", mediator.getPage(""));
    }

    @Test
    public void pageTest_null() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals("", mediator.getPage(null));
    }

    @Test
    public void pageTest_emptyReturn() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Assert.assertEquals("", mediator.getPage(" "));
    }

    @Test
    public void pageTest_cache() {
        WikiMediator mediator = new WikiMediator(10, 30);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        mediator.getPage("Dog");
        String dog = mediator.getPage("Dog");

        Assert.assertEquals(wiki.getPageText("Dog"), dog);
    }

    @Test
    public void pageTest_expiredCache() {
        WikiMediator mediator = new WikiMediator(10, 1);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        mediator.getPage("Dog");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        String dog = mediator.getPage("Dog");

        Assert.assertEquals(wiki.getPageText("Dog"), dog);
    }

    // Should expect to see 3 JWiki requests of dog
    // Because the cache is full
    @Test
    public void pageTest_fullCache() {
        WikiMediator mediator = new WikiMediator(2, 30);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        mediator.getPage("Dog");
        mediator.getPage("Cat");
        mediator.getPage("Bird");

        String dog = mediator.getPage("Dog");

        Assert.assertEquals(wiki.getPageText("Dog"), dog);
    }


    @Test
    public void zeitgeistTest() {
        WikiMediator mediator = new WikiMediator(10, 30);
        List<String> obama = mediator.search("Obama", 3);
        mediator.getPage(obama.get(0));
        mediator.getPage(obama.get(0));
        mediator.getPage(obama.get(1));
        mediator.getPage(obama.get(1));
        mediator.getPage(obama.get(1));
        mediator.getPage(obama.get(2));
        mediator.getPage(obama.get(2));

        Assert.assertEquals(List.of(
                obama.get(1), obama.get(2), obama.get(0), "Obama"),
            mediator.zeitgeist(10)
        );
    }

    @Test
    public void zeitgeist_limit() {
        WikiMediator mediator = new WikiMediator(10, 30);
        // Note that cat.get(0) == "Cat"
        List<String> cat = mediator.search("Cat", 3);
        mediator.getPage(cat.get(0));
        mediator.getPage(cat.get(0));
        mediator.getPage(cat.get(1));
        mediator.getPage(cat.get(1));
        mediator.getPage(cat.get(1));
        mediator.getPage(cat.get(2));
        mediator.getPage(cat.get(2));

        Assert.assertEquals(Arrays.asList(cat.get(1), cat.get(0)), mediator.zeitgeist(2));
    }

    @Test
    public void zeitgeist_zeroLimit() {
        WikiMediator mediator = new WikiMediator(10, 30);
        // Note that cat.get(0) == "Cat"
        List<String> cat = mediator.search("Cat", 3);
        mediator.getPage(cat.get(0));
        mediator.getPage(cat.get(0));
        mediator.getPage(cat.get(1));
        mediator.getPage(cat.get(1));
        mediator.getPage(cat.get(1));
        mediator.getPage(cat.get(2));
        mediator.getPage(cat.get(2));

        Assert.assertEquals(List.of(), mediator.zeitgeist(0));
    }

    @Test
    public void zeitgeist_noRequests() {
        WikiMediator mediator = new WikiMediator(10, 30);

        Assert.assertEquals(List.of(), mediator.zeitgeist(10));
    }

    @Test
    public void zeitgeist_allSameRequests() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Demon Cat");
        mediator.getPage("Demon Cat");
        mediator.getPage("Demon Cat");

        Assert.assertEquals(List.of("Demon Cat"), mediator.zeitgeist(10));
    }

    @Test
    public void zeitgeist_alphabet() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("A");
        mediator.getPage("B");
        mediator.getPage("C");
        mediator.getPage("D");
        mediator.getPage("E");
        mediator.getPage("F");
        mediator.getPage("G");
        mediator.getPage("H");
        mediator.getPage("I");

        Assert.assertEquals(List.of("I", "H", "G", "F", "E", "D", "C", "B", "A"),
            mediator.zeitgeist(10));
    }

    @Test
    public void trendingTest() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Cats");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");

        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        mediator.getPage("Cats");
        mediator.getPage("Cats");

        Assert.assertEquals(List.of("Cats"), mediator.trending(2, 2));
    }

    @Test
    public void trendingTest_zeroLimit() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Cats");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");

        mediator.getPage("Cats");
        mediator.getPage("Cats");

        Assert.assertEquals(List.of(), mediator.trending(3, 0));
    }

    @Test
    public void trendingTest_zeroWindow() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Cats");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");

        mediator.getPage("Cats");
        mediator.getPage("Cats");

        Assert.assertEquals(List.of(), mediator.trending(0, 10));
    }

    @Test
    public void trendingTest_alphabet() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("A");
        mediator.getPage("B");
        mediator.getPage("C");
        mediator.getPage("D");
        mediator.getPage("E");
        mediator.getPage("F");
        mediator.getPage("G");
        mediator.getPage("H");
        mediator.getPage("I");

        Assert.assertEquals(List.of("I", "H", "G", "F", "E", "D", "C", "B", "A"),
            mediator.trending(10, 10));
    }


    @Test
    public void trendingTest_bigWindow() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Cats");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        mediator.getPage("Cats");
        mediator.getPage("Dogs");
        mediator.getPage("Cats");

        Assert.assertEquals(List.of("Cats", "Dogs"), mediator.trending(1, 10));
    }

    @Test
    public void trendingTest_sameSearchWithDelay() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Cats");
        mediator.getPage("Dogs");
        mediator.getPage("Dogs");
        mediator.getPage("Hamster");
        mediator.search("Owl", 1);
        mediator.getPage("Dogs");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }


        mediator.search("Alligator", 1);
        mediator.search("Crocodile", 1);
        mediator.getPage("Dogs");
        mediator.getPage("Zebra");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        mediator.getPage("Cats");
        mediator.getPage("Cats");
        Assert.assertEquals(List.of("Cats", "Crocodile", "Dogs", "Zebra", "Alligator"),
            mediator.trending(3, 10));
    }

    @Test
    public void trendingTest_sameRequests() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Demon Cat");
        mediator.getPage("Demon Cat");
        mediator.getPage("Demon Cat");
        mediator.getPage("Demon Cat");

        Assert.assertEquals(List.of("Demon Cat"), mediator.trending(2, 10));
    }

    @Test
    public void windowedPeakLoadTest() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Dog");
        mediator.getPage("Cat");
        mediator.getPage("Bird");
        mediator.getPage("Rat");
        mediator.getPage("Hamster");
        mediator.getPage("Porcupine");
        mediator.getPage("Ant");
        mediator.getPage("Snake");
        try {
            Thread.sleep(4 * 1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        mediator.search("Giraffe", 1);
        mediator.search("Zebra", 1);
        mediator.search("Lion", 1);
        mediator.windowedPeakLoad();

        Assert.assertEquals(8, mediator.windowedPeakLoad(3));

    }

    @Test
    public void windowedPeakLoadTest_endingLoad() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.search("Giraffe", 1);
        mediator.search("Zebra", 1);
        mediator.search("Lion", 1);
        mediator.windowedPeakLoad();

        try {
            Thread.sleep(4 * 1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        mediator.getPage("Dog");
        mediator.getPage("Cat");
        mediator.getPage("Bird");
        mediator.getPage("Rat");
        mediator.getPage("Hamster");
        mediator.getPage("Porcupine");
        mediator.getPage("Ant");
        mediator.getPage("Snake");

        Assert.assertEquals(9, mediator.windowedPeakLoad(3));

    }

    @Test
    public void windowedPeakLoadTest_zeroWindow() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.search("Giraffe", 1);
        mediator.search("Zebra", 1);
        mediator.search("Lion", 1);


        Assert.assertEquals(0, mediator.windowedPeakLoad(0));

    }

    @Test
    public void windowedPeakLoadTest_singularReqs() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.search("Giraffe", 1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        mediator.search("Zebra", 1);
        mediator.search("Antelope", 1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        mediator.search("Lion", 1);
        Assert.assertEquals(2, mediator.windowedPeakLoad(1));

    }


    @Test
    public void windowedPeakLoadTest_windowSeperation() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.search("Giraffe", 1);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        mediator.search("Lion", 1);
        Assert.assertEquals(2, mediator.windowedPeakLoad(3));

    }

    // The following tests are commented out because they affect the test above
    // These tests should be run individually, and created files should
    // be deleted
    @Test
    public void jsonTest() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Dog");
        mediator.getPage("Dog");
        mediator.getPage("Dog");
        mediator.zeitgeist(2);
        mediator.trending(5, 2);
        mediator.windowedPeakLoad();
        try {
            mediator.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File zeitgeist = new File("local/zeitgeistData.json");
        File trending = new File("local/trendingData.json");
        File peakLoad = new File("local/peakLoadData.json");

        if (zeitgeist.delete()) {
            System.out.println("Deleted zeitgeistData.json");
        }
        if (trending.delete()) {
            System.out.println("Deleted trendingData.json");
        }
        if (peakLoad.delete()) {
            System.out.println("Deleted peakLoadData.json");
        }
    }

    @Test
    public void jsonTest_reading() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Dog");
        mediator.getPage("Dog");
        mediator.getPage("Dog");
        mediator.getPage("Cat");
        mediator.getPage("Cat");
        mediator.zeitgeist(2);
        mediator.trending(10, 10);
        mediator.windowedPeakLoad();
        try {
            mediator.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        WikiMediator mediator1 = new WikiMediator(10, 30);
        mediator1.search("Mouse", 1);
        Assert.assertEquals(List.of("Dog", "Cat", "Mouse"), mediator1.zeitgeist(3));
        Assert.assertEquals(11, mediator1.windowedPeakLoad());
        Assert.assertEquals(List.of("Dog", "Cat", "Mouse"), mediator1.trending(30, 5));


        File zeitgeist = new File("local/zeitgeistData.json");
        File trending = new File("local/trendingData.json");
        File peakLoad = new File("local/peakLoadData.json");

        if (zeitgeist.delete()) {
            System.out.println("Deleted zeitgeistData.json");
        }
        if (trending.delete()) {
            System.out.println("Deleted trendingData.json");
        }
        if (peakLoad.delete()) {
            System.out.println("Deleted peakLoadData.json");
        }
    }

    @Test
    public void json_twoClose() {
        WikiMediator mediator = new WikiMediator(10, 30);
        mediator.getPage("Dog");
        mediator.getPage("Dog");
        mediator.getPage("Dog");
        mediator.getPage("Cat");
        mediator.getPage("Cat");
        mediator.zeitgeist(2);
        mediator.trending(10, 10);
        mediator.windowedPeakLoad();
        try {
            mediator.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        WikiMediator mediator1 = new WikiMediator(10, 30);
        mediator1.search("Mouse", 1);
        mediator1.search("Cat", 1);
        mediator1.search("Cat", 1);


        try {
            mediator1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        WikiMediator mediator2 = new WikiMediator(10, 30);

        mediator2.search("Mouse", 1);
        mediator2.getPage("Giraffe");
        mediator2.search("Mouse", 1);
        mediator2.search("Mouse", 1);
        mediator2.search("Cat", 1);

        Assert.assertEquals(List.of("Cat", "Mouse", "Dog", "Giraffe"), mediator2.zeitgeist(4));
        Assert.assertEquals(8, mediator2.windowedPeakLoad(2));
        Assert.assertEquals(List.of("Mouse", "Giraffe", "Cat"), mediator2.trending(2, 5));


        File zeitgeist = new File("local/zeitgeistData.json");
        File trending = new File("local/trendingData.json");
        File peakLoad = new File("local/peakLoadData.json");

        if (zeitgeist.delete()) {
            System.out.println("Deleted zeitgeistData.json");
        }
        if (trending.delete()) {
            System.out.println("Deleted trendingData.json");
        }
        if (peakLoad.delete()) {
            System.out.println("Deleted peakLoadData.json");
        }
    }

}

/*
    Note: the task 4 tests were done inside WikiMediatorClient to ensure proper thread behaviour,
    avoiding complications with JUnit.
     */
