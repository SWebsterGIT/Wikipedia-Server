package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.fastily.jwiki.core.Wiki;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Task5Tests {

    @Test
    public void OneStep() throws TimeoutException{
        WikiMediator test = new WikiMediator(300,300);

        List<String> expected = new ArrayList<>(List.of("BC Wildfire Service", "Abbotsford, British Columbia"));

        List<String> result = test.shortestPath("BC Wildfire Service", "Abbotsford, British Columbia", 30000000);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void TwoSteps() throws TimeoutException{
        WikiMediator test = new WikiMediator(300,300);

        List<String> expected = new ArrayList<>(List.of("Philosophy", "Academic bias", "Barack Obama"));

        List<String> result = test.shortestPath("Philosophy", "Barack Obama", 30000000);

        Assert.assertEquals(expected, result);

    }

    @Test
    public void ThreeSteps() throws TimeoutException{
        WikiMediator test = new WikiMediator(300,300);
        List<String> expected = new ArrayList<>(List.of("BC Wildfire Service", "Abbotsford, British Columbia", "Black people", "Barack Obama"));

        List<String> result = test.shortestPath("BC Wildfire Service", "Barack Obama", 30000000);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void LongStartingPage() throws TimeoutException{
        WikiMediator test = new WikiMediator(300,300);
        List<String> expected = new ArrayList<>(List.of("United States", "Ariana Grande", "Travis Scott"));

        List<String> result = test.shortestPath("United States", "Travis Scott", 30000000);

        Assert.assertEquals(expected, result);
    }

    @Test (expected = TimeoutException.class)
    public void timeout() throws TimeoutException{
        WikiMediator test = new WikiMediator(300, 300);

        test.shortestPath("United States", "Travis Scott", 5);
    }

    @Test
    public void noChildren() throws TimeoutException{
        WikiMediator test = new WikiMediator(300,300);

        List<String> result = test.shortestPath("Barack Obama", "auwfhiuawhefiuawehf", 1000000);

        Assert.assertEquals(new ArrayList<>(), result);
    }

    @Test
    public void threeDegrees() throws TimeoutException{
        WikiMediator test = new WikiMediator(300,300);

        List<String> result = test.shortestPath("Puntzi Lake", "MV Spirit of British Columbia", 1000000);

        List<String> expected = new ArrayList<>(List.of("Puntzi Lake", "Active Pass", "BC Ferries", "MV Spirit of British Columbia"));

        Assert.assertEquals(expected, result);
    }

    @Test
    public void anotherTest() throws TimeoutException{
        WikiMediator test = new WikiMediator(300,300);

        List<String> result = test.shortestPath("Puntzi Lake", "Severe acute respiratory syndrome", 30000000);

        List<String> expected = new ArrayList<>(List.of("Puntzi Lake", "1862 Pacific Northwest smallpox epidemic", "2002–2004 SARS outbreak", "Severe acute respiratory syndrome"));

        Assert.assertEquals(expected, result);
    }

    @Test
    public void sameAsOrigin() throws TimeoutException{
        WikiMediator test = new WikiMediator(300, 300);

        List<String> result = test.shortestPath("BC Wildfire Service", "BC Wildfire Service",300);

        Assert.assertEquals(List.of("BC Wildfire Service"),result);
    }


}

/*
    Note: the task 4 tests were done inside WikiMediatorClient to ensure proper thread behaviour,
    avoiding complications with JUnit.
     */