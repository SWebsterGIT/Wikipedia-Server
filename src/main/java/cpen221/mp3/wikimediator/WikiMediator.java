package cpen221.mp3.wikimediator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.wikiTree.Node;
import org.fastily.jwiki.core.Wiki;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class WikiMediator implements AutoCloseable {
    /* Representation Invariant:
     * Total number of times in requestHistory <= basicReqHistory.size()
     * cache size <= basicReqTimes.size()
     * sum of queryTimes.values.size() <= basicReqTimes()
     * requestHistory.keySet() will contain all queryTimes.keySet()
     * All elements contained within queryTimes.values() should be in
     * basicReqTimes
     */

    /* Abstraction Function:
     * Represents the mediator between a user and Wikipedia.
     * A user should be able to easily interact with Wikipedia, using
     * this mediator. Previously requested pages are represented as
     * Page objects in a finite size finite time buffer.
     *
     * Request times are represented using the time in milliseconds from January
     * 1970 at midnight UTC.
     *
     * Multiple requests to a mediator can occur at the same time.
     */

    /* Thread Safety Arguments:
     * The class is thread safe because it implements,
     * thread safe data types:
     * - Avoids bad interleaving by using streams
     * - read and writes that may affect thread safety are synchronized
     * - Update times are synchronized
     * - No methods support removal from data structures
     */

    /**
     * finite size finite time buffer to store pages
     */
    private final FSFTBuffer<Page> cache;
    /**
     * API connection to Wikipedia
     */
    private final Wiki wiki;

    /**
     * search and getPage requests used in zeitgeist
     */
    private Map<String, Integer> requestHistory;
    /**
     * queries from search and getPage with their times used in trending
     */
    private Map<String, List<Long>> queryTimes;
    /**
     * basic request times used in windowedPeakLoad
     */
    private List<Long> basicReqTimes;

    /**
     * Creates a mediator service to cache wikipedia pages.
     * Stores a maximum of {@code capacity} number of pages that have a cache
     * time that is less than {@code stalenessInterval}.
     * Once a page reaches its {@code stalenessInterval} within the cache,
     * it is removed. When at max capacity the oldest pages in the cache are
     * removed once any new page is added.
     *
     * @param capacity          maximum number of pages that can be stored in
     *                          the cache.
     * @param stalenessInterval maximum time that a page can be stored in the
     *                          cache.
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        this.wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        this.cache = new FSFTBuffer<>(capacity, stalenessInterval);
        readData();
        //checkRep();
    }

    /**
     * Reads request data from JSON files, in the local directory
     */
    public synchronized void readData() {
        Gson gson = new Gson();

        // Get zeitgeist data
        try {
            Reader reader = Files.newBufferedReader(Paths
                .get("local/zeitgeistData.json"));
            Map<String, Integer> zeitgeist = gson.fromJson(reader,
                new TypeToken<Map<String, Integer>>() {
                }.getType());
            reader.close();
            try {
                requestHistory = new ConcurrentHashMap<>(zeitgeist);
            } catch (NullPointerException npe) {
                requestHistory = new ConcurrentHashMap<>();
            }
        } catch (IOException ioException) {
            requestHistory = new ConcurrentHashMap<>();
        }

        // Get trending data
        try {
            Reader reader = Files.newBufferedReader(Paths
                .get("local/trendingData.json"));
            Map<String, List<Long>> trending = gson.fromJson(reader,
                new TypeToken<Map<String, List<Long>>>() {
                }.getType());
            reader.close();
            try {
                queryTimes = new ConcurrentHashMap<>(trending);
            } catch (NullPointerException npe) {
                queryTimes = new ConcurrentHashMap<>();
            }
        } catch (IOException ioException) {
            queryTimes = new ConcurrentHashMap<>();
        }

        // Get windowedPeakLoad data
        try {
            Reader reader = Files.newBufferedReader(Paths
                .get("local/peakLoadData.json"));
            List<Long> windowed = gson.fromJson(reader,
                new TypeToken<List<Long>>() {
                }.getType());
            reader.close();
            try {
                basicReqTimes = new ArrayList<>(windowed);
            } catch (NullPointerException npe) {
                basicReqTimes = new ArrayList<>();
            }
        } catch (IOException ioException) {
            basicReqTimes = new ArrayList<>();
        }
    }

    /**
     * From a given query, return a list of maximum length {@code limit} of
     * Page titles
     * that contain the query.
     *
     * @param query title to search for on Wikipedia
     * @param limit maximum number of pages containing query returned in a List.
     *              if -1, full list of results is returned.
     *              must be >= -1.
     * @return a List with max length of {@code limit} of Wikipedia pages that
     * the String {@code query}. Returns empty list if no results for a query,
     * or if limit == 0.
     */
    public List<String> search(String query, int limit) {
        long requestTime;
        synchronized (this) {
            requestTime = System.currentTimeMillis();
            basicReqTimes.add(requestTime);
        }

        if (query == null || query.isEmpty() || limit == 0) {
            return new ArrayList<>();
        }


        if (!queryTimes.containsKey(query)) {
            queryTimes.put(query, new ArrayList<>());
        }
        queryTimes.get(query).add(requestTime);


        List<String> search = wiki.search(query, limit);
        int requestCount = requestHistory.getOrDefault(query, 0);
        this.requestHistory.put(query, ++requestCount);

        return new ArrayList<>(search);
    }

    /**
     * Given {@code pageTitle}, return the text of the Wikipedia page with the
     * title {@code pageTitle}.
     *
     * @param pageTitle title of the Wikipedia page to be found
     * @return the text contained in the Wikipedia page of title
     * {@code pageTitle}, if the page does not exist returns an empty String
     */
    public String getPage(String pageTitle) {
        long requestTime;
        synchronized (this) {
            requestTime = System.currentTimeMillis();
            basicReqTimes.add(requestTime);
        }

        if (pageTitle == null || pageTitle.isEmpty()) {
            return "";
        }

        synchronized (this) {
            if (!queryTimes.containsKey(pageTitle)) {
                queryTimes.put(pageTitle, new ArrayList<>());
            }
            queryTimes.get(pageTitle).add(requestTime);
        }

        String pageText;
        try {
            pageText = cache.get(pageTitle).getText();
        } catch (NoSuchElementException nse) {
            pageText = wiki.getPageText(pageTitle);
            cache.put(new Page(pageTitle, pageText));
        }

        synchronized (this) {
            int requestCount = requestHistory.getOrDefault(pageTitle, 0);
            requestHistory.put(pageTitle, ++requestCount);
        }
        return pageText;
    }

    /**
     * Returns the most common query and pageTitle Strings used in
     * {@code search} and {@code getPage}. Items in the returned
     * list are sorted in non-increasing
     * based on their search count. The list returned is of max length
     * {@code limit}.
     *
     * @param limit maximum size of the returned list of pages
     * @return a list of the most common occurring Strings of
     * {@code query} and {@code pageTitle} called in {@code search} and
     * {@code getPage}, if there is a tie queries ares sorted in
     * anti-lexicographical
     */
    public List<String> zeitgeist(int limit) {
        synchronized (this) {
            long requestTime = System.currentTimeMillis();
            basicReqTimes.add(requestTime);
        }


        //Creates a list of keys sorted in non-decreasing order by their values
        List<String> reqHis;

        synchronized (this) {
            reqHis = requestHistory.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());


            Collections.reverse(reqHis);
        }

        if (reqHis.size() >= limit) {
            return new ArrayList<>(reqHis.subList(0, limit));
        } else {
            return new ArrayList<>(reqHis);
        }
    }

    /**
     * Returns the most common occurring {@code query} and {@code getPage}
     * Strings from {@code search} and {@code getPage}, within the past
     * {@code timeLimitInSeconds} seconds. The returned list has a max
     * length of {@code maxItems} of the most frequent requests sorted
     * by non-increasing order.
     *
     * @param timeLimitInSeconds time limit in which to find the most frequent
     *                           occurring requests
     * @param maxItems           maximum number of items to be included in
     *                           the returned
     *                           list of most frequent requests
     * @return list of the most frequent requests of Strings made by
     * {@code search} and {@code getPage} in non-increasing order,
     * If there is a tie, ordering between ties is non-deterministic
     */
    public List<String> trending(int timeLimitInSeconds, int maxItems) {
        long requestTime;
        synchronized (this) {
            requestTime = System.currentTimeMillis();
            basicReqTimes.add(requestTime);
        }

        Map<String, Integer> trendingQueries = new HashMap<>();
        List<String> trending;

        synchronized (this) {
            for (String query : queryTimes.keySet()) {
                trendingQueries.put(query, (int) queryTimes.get(query).stream()
                    .filter(
                        time -> time > requestTime - TimeUnit.SECONDS
                            .toMillis(timeLimitInSeconds)).count());
                if (trendingQueries.get(query) == 0) {
                    trendingQueries.remove(query);
                }
            }

            trending = trendingQueries.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(maxItems)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }


        Collections.reverse(trending);

        if (trending.size() >= maxItems) {
            return new ArrayList<>(trending.subList(0, maxItems));
        } else {
            return new ArrayList<>(trending);
        }
    }

    /**
     * Returns the maximum number of basic requests seen within a given time
     * window.
     * Any methods the request information of a Wiki page is considered a basic
     * request. This current request of the {@code timeWindowInSeconds} IS
     * included the results of this call
     *
     * @param timeWindowInSeconds time window to find the maximum number of
     *                            requests made
     * @return the max number of requests seen in a {@code timeWindowInSeconds}
     */
    public int windowedPeakLoad(int timeWindowInSeconds) {
        synchronized (this) {
            long requestTime = System.currentTimeMillis();
            basicReqTimes.add(requestTime);
        }


        int requestCounter = 0;
        int maxRequests = requestCounter;

        synchronized (this) {
            for (Long request : basicReqTimes) {
                long startTime = request;
                List<Long> timeWindowReq = basicReqTimes.stream()
                    .filter(time -> (time >= startTime &&
                        (time < (startTime + TimeUnit.SECONDS
                            .toMillis(timeWindowInSeconds)))))
                    .collect(Collectors.toList());

                requestCounter = timeWindowReq.size();
                if (requestCounter > maxRequests) {
                    maxRequests = requestCounter;
                }
            }
        }


        return maxRequests;
    }

    /**
     * Returns the maximum number of basic requests seen within a 30-second
     * time window.
     * Any methods the request information of a Wiki page is considered a basic
     * request.
     *
     * @return the max number of requests seen in a time window of 30-seconds.
     * This includes the current call of {@code windowedPeakLoad}
     */
    public int windowedPeakLoad() {
        return windowedPeakLoad(30);
    }

    /**
     * Finds the shortest path from {@code pageTitle1} wiki page to
     * {@code pageTitle2} wiki page, path is made of wikipedia page links that
     * are in an article.
     *
     * @param pageTitle1 title of first page
     * @param pageTitle2 title of second page to be linked to
     * {@code pageTitle1}
     * @param timeout    time in seconds permitted for this operation to run
     * @return The shortest path of wikipedia page links that need to be
     * clicked in order to travel from {@code pageTitle1} to {@code pageTitle2}. In the case that there is no possible
     * path, an empty list will be returned. In the case that pageTitle1 is the same as pageTitle2, a list of size 1
     * will be returned with contents pageTitle1.
     * @throws TimeoutException if the operation exceeds {@code timeout} elapsed time.
     */
    public List<String> shortestPath(String pageTitle1, String pageTitle2, int timeout) throws
        TimeoutException {

        Node head = new Node(pageTitle1, pageTitle2, timeout);

        if(wiki.getLinksOnPage(pageTitle1).size() == 0 || wiki.whatLinksHere(pageTitle2).size() == 0){
            return new ArrayList<>();
        }

        try {
            head.buildTree();
            return head.getDestinationPath();
        } catch (TimeoutException te) {
            throw new TimeoutException();
        }

    }

    /**
     * Writes request data used in zeitgeist, trending, and peakLoad to JSON
     * files in local.
     * Should be called whenever a thread running an instance of
     * {@code WikiMediator} is closed.
     *
     * @throws Exception if failed to close for whatever reason
     */
    @Override
    public void close() {
        try {
            Gson gson = new Gson();
            Writer zeitgeist = new FileWriter("local/zeitgeistData.json");
            gson.toJson(requestHistory, zeitgeist);
            zeitgeist.close();

            Writer trending = new FileWriter("local/trendingData.json");
            gson.toJson(queryTimes, trending);
            trending.close();

            Writer peakLoad = new FileWriter("local/peakLoadData.json");
            gson.toJson(basicReqTimes, peakLoad);
            peakLoad.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * Checks to ensure that the representation invariant is not broken
     */
    private synchronized void checkRep() {
        assert requestHistory.size() <= basicReqTimes.size();

        assert cache.size() <= basicReqTimes.size();


        int timeCount = 0;
        for(String query : queryTimes.keySet()) {
            timeCount += queryTimes.get(query).size();
        }

        assert timeCount <= basicReqTimes.size();

        for(String query : queryTimes.keySet()) {
            assert requestHistory.containsKey(query);
        }


        for(String query : queryTimes.keySet()) {
            for(int i = 0; i < queryTimes.get(query).size(); i++) {
                assert basicReqTimes.contains(queryTimes.get(query).get(i));
            }
        }

    }
}
