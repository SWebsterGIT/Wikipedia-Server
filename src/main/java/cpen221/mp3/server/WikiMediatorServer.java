package cpen221.mp3.server;

import cpen221.mp3.wikimediator.WikiMediator;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


public class WikiMediatorServer {


    /*
    RI: none of the instance variables should be "null".

    maxNumUsers is > 0. (otherwise the server would support a maximum of less than 1 client,
    effectively meaning it would not be able to serve anything...)

    currentNumUsers >= 0 (can't have negative users).

    currentNumUsers <= maxNumUsers (this is enforced through the main serve() while loop).

    wikiMediator has the valid history (enforced by the WM constructor).


    AF: Represents a server, wrapping an instance of a wikiMediator, with a maximum
    concurrent client pool of maxNumUsers, current connections of currentNumUsers,
    a socket connection serverSocket, which continues running until shutdown is true and
    continueRunning is false.

     */

    public static final int SERVER_PORT = 9001;

    private boolean continueRunning;
    private boolean shutdown;
    private ServerSocket serverSocket;
    private WikiMediator wikiMediator;
    private final int maxNumUsers;
    private volatile int currentNumUsers;

    /*
    ThreadSafety Arguments:

    continueRunning: will only be accessed inside the main thread and so cannot be accessed
    in any way other than sequentially

    shutdown: uses a lock to ensure reading and writing cannot happen simultaneously

    ServerSocket: will only be accessed inside the main thread and so cannot be accessed
    in any way other than sequentially

    wikiMediator: calls to methods inside this do not use anything other than locally created variables,
    so they will be housed in each thread's stack and not exposed. Further, this is a threadsafe datatype
    as it has been guaranteed as such when defined by our group

    maxNumUsers: is immutable, by virtue of being a final primitive type

    currentNumUsers: is declared  volatile due to issues with infiinite looping if we try to block new connections
    while reading it, and cannot be written to by more than one thread at one (locked)

     */


    // was used to start up the server for testing purposes.
//    public static void main(String[] args) {
//        try {
//            WikiMediator wikiMed = new WikiMediator(CAPACITY,STALENESS_INTERVAL);
//
//            WikiMediatorServer server = new WikiMediatorServer(SERVER_PORT,NUM_CONCURRENT_REQS,wikiMed);
//            server.serve();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Start a server at a given port number, with the ability to process
     * upto n requests concurrently.
     *
     * @param port the port number to bind the server to, 9000 <= {@code port} <= 9999
     * @param n the number of concurrent requests the server can handle, 0 < {@code n} <= 32
     * @param wikiMediator the WikiMediator instance to use for the server, {@code wikiMediator} is not {@code null}
     */
    public WikiMediatorServer(int port, int n, WikiMediator wikiMediator) {

        try{
            serverSocket = new ServerSocket(port);
        }
        catch(IOException ioe){
            throw new RuntimeException();
        }

        this.wikiMediator = wikiMediator;
        this.maxNumUsers = n;
        this.currentNumUsers = 0;
        this.continueRunning = true;
    }

    /**
     * Run the server, listening for connections and handling them.
     *
     * @throws IOException
     *             if the main server socket is broken
     */
    public void serve() throws IOException {
        while (continueRunning) {
            // block until a client connects
            final Socket socket = serverSocket.accept();
            // create a new thread to handle that client

            // block new requests being handled if we are at the maximum number of clients already
            while(currentNumUsers >= maxNumUsers){
            }


            Thread handler = new Thread(new Runnable() {
                public void run() {
                    try {
                        try {
                            handle(socket);
                            } finally {
                                socket.close();
                                synchronized (this){
                                    currentNumUsers--;
                                }
                            }
                        } catch (IOException ioe) {
                            // this exception wouldn't terminate serve(),
                            // since we're now on a different thread, but
                            // we still need to handle it
                            ioe.printStackTrace();
                        }
                    }
                });
                // start the thread

            synchronized (this){
                if(shutdown == true){
                    continueRunning = false;

                    // we would like to send the client that just tried to connect an error message
                    // subsequent clients, however, will just trigger an exception, as the server
                    // socket will have been closed by then.

                    PrintWriter out = new PrintWriter(new OutputStreamWriter(
                        socket.getOutputStream()));

                    out.println("The server has been shut down... Subsequent requests will" +
                        " throw exceptions... \n");

                    out.flush();

                }else{
                    currentNumUsers++;
                    handler.start();
                }
            }
        }
        serverSocket.close();
    }

    /**
     * Handle one client connection. Returns when client disconnects.
     *
     * @param socket
     *            socket where client is connected
     * @throws IOException
     *             if connection encounters an error
     */

    private void handle(Socket socket) throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(
            socket.getOutputStream()));
        ExecutorService reqExecutor = Executors.newSingleThreadExecutor();

        try {
            // each request is a single line containing a JSON
            for (String line = in.readLine(); line != null; line = in
                .readLine()) {

                String jsonReq = line;

                    // interpret request
                Gson gson = new Gson();
                JsonObject reqObj = gson.fromJson(jsonReq,JsonObject.class);


                Future<String> reqResult = reqExecutor.submit(new Callable<>() {
                    @Override
                    public String call(){
                        return getResponse(reqObj);
                    }
                });

                if(!reqObj.has("timeout")) {

                    try {

                        String jsonResponse = reqResult.get();
                        out.println(jsonResponse);

                    } catch (InterruptedException | CancellationException | ExecutionException ex) {

                        ex.printStackTrace();
                        throw new RuntimeException();
                    }
                }else {
                    int timeout = reqObj.get("timeout").getAsInt();
                    try{

                        String jsonResponse = reqResult.get(timeout, TimeUnit.SECONDS);
                        out.println(jsonResponse);

                    }catch(InterruptedException | CancellationException | ExecutionException ex){

                        ex.printStackTrace();
                        throw new RuntimeException();
                    }catch(TimeoutException ex){

                        JsonObject ObjResponse = new JsonObject();
                        ObjResponse.addProperty("id",reqObj.get("id").getAsString());
                        ObjResponse.addProperty("status", "failed");
                        ObjResponse.addProperty("response", "Operation timed out");

                        String jsonResponse = gson.toJson(ObjResponse);
                        out.println(jsonResponse);
                    }

                }

                // important! flush our buffer so the reply is sent
                out.flush();
            }
        } finally {
            out.close();
            in.close();
        }
    }

    private String getResponse(JsonObject reqObj){

        Gson gson = new Gson();

        String methodToCall = reqObj.get("type").getAsString();
        String id = reqObj.get("id").getAsString();

        // compute answer as a Json and send back to caller
        if(methodToCall.equals("search")){

            String query = reqObj.get("query").getAsString();
            int limit = Integer.parseInt(reqObj.get("limit").getAsString());

            List<String> searchResult = wikiMediator.search(query,limit);


            String status = "success";

            ListResponse replyObj = new ListResponse(id,status,searchResult);


            String replyJSON = gson.toJson(replyObj);


            return replyJSON;

        }else if(methodToCall.equals("getPage")){

            String pageTitle = reqObj.get("pageTitle").getAsString();

            String getPageResult = wikiMediator.getPage(pageTitle);
            String status = "success";

            // assemble the object to be used as a reply
            StringResponse replyObj = new StringResponse(id,status,getPageResult);

            String replyJSON = gson.toJson(replyObj);


            return replyJSON;

        }else if(methodToCall.equals("zeitgeist")){

            int limit = Integer.parseInt(reqObj.get("limit").getAsString());

            List<String> zeitgeistResult = wikiMediator.zeitgeist(limit);

            String status = "success";

            // assemble the object to be used as a reply
            ListResponse replyObj = new ListResponse(id,status,zeitgeistResult);

            String replyJSON = gson.toJson(replyObj);


            return replyJSON;

        }else if(methodToCall.equals("trending")){

            int timeLimitInSeconds =reqObj.get("timeLimitInSeconds").getAsInt();
            int maxItems =reqObj.get("maxItems").getAsInt();

            List<String> trendingResult = wikiMediator.trending(timeLimitInSeconds, maxItems);

            String status = "success";

            // assemble the object to be used as a reply
            ListResponse replyObj = new ListResponse(id,status,trendingResult);

            String replyJSON = gson.toJson(replyObj);

            return replyJSON;

        }else if(methodToCall.equals("windowedPeakLoad")){

            int WPLResult;

            if (reqObj.has("timeWindowInSeconds")){
                int timeWindowInSeconds =reqObj.get("timeWindowInSeconds").getAsInt();
                WPLResult = wikiMediator.windowedPeakLoad(timeWindowInSeconds);
            }
            else{
                WPLResult = wikiMediator.windowedPeakLoad();
            }

            String status = "success";
            // assemble the object to be used as a reply
            IntResponse replyObj = new IntResponse(id,status,WPLResult);

            String replyJSON = gson.toJson(replyObj);

            return replyJSON;
        }else if(methodToCall.equals("shortestPath")){

            String pageTitle1 = reqObj.get("pageTitle1").getAsString();
            String pageTitle2 = reqObj.get("pageTitle2").getAsString();

            int timeout =reqObj.get("timeout").getAsInt();

            try{
                List<String> SPResult = wikiMediator.shortestPath(pageTitle1,pageTitle2,timeout);

                String status = "success";

                // assemble the object to be used as a reply
                ListResponse replyObj = new ListResponse(id,status,SPResult);

                String replyJSON = gson.toJson(replyObj);

                return replyJSON;

            }catch(TimeoutException te){
                JsonObject ObjResponse = new JsonObject();
                ObjResponse.addProperty("id",reqObj.get("id").getAsString());
                ObjResponse.addProperty("status", "failed");
                ObjResponse.addProperty("response", "Operation timed out");

                String timeoutResponse = gson.toJson(ObjResponse);
                return timeoutResponse;
            }

        }else if(methodToCall.equals("stop")){

            // assemble the object to be used as a reply
            StopResponse replyObj = new StopResponse(id,"bye");

            String replyJSON = gson.toJson(replyObj);

            // This writes the required data to the disk in /local, so data can survive
            wikiMediator.close();

            // this will cause the loop in serve() to break and the server socket to close
            synchronized (this){
                shutdown = true;
            }
            return replyJSON;
        }

        // this will never trigger, since requests will be well formatted.
        return "ill-formatted response";

    }

}

class ListResponse{

    private final String id;
    private final String status;
    private final List<String> response;

    public ListResponse(String id, String status, List<String> response){
        this.id = id;
        this.status = status;
        this.response = response;
    }
}

class StringResponse{

    private final String id;
    private final String status;
    private final String response;

    public StringResponse(String id, String status, String response){
        this.id = id;
        this.status = status;
        this.response = response;
    }
}

class IntResponse{

    private final String id;
    private final String status;
    private final int response;

    public IntResponse(String id, String status, int response){
        this.id = id;
        this.status = status;
        this.response = response;
    }
}

class StopResponse{
    private final String id;
    private final String response;

    public StopResponse(String id, String response){
        this.id = id;
        this.response = response;
    }
}

