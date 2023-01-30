package cpen221.mp3.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class WikiMediatorClient {

    private Socket socket;
    private BufferedReader in;
    // Rep invariant: socket, in, out != null, clientID is any integer
    // (since it is only used for tracking, no reason it shouldn't be negative.)
    // AF: represents a client for the server, connected at "socket", with an ID
    // (for tracking purposes only) of clientID
    private PrintWriter out;
    public final int clientID;

    //TODO: Test shortestpath by itself. Test a big search with a short timelimit to see it time out

    /**
     * Make a WikiMediatorClient and connect it to a server running on
     * hostname at the specified port.
     *
     * @throws IOException if can't connect
     */
    public WikiMediatorClient(String hostname, int port, int clientID) throws IOException {
        socket = new Socket(hostname, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.clientID = clientID;

    }
    /**
     * Use a WikiMediatorServer to find the results for one of each of the various wikiMediator methods
     *
     * This traces through the various paths and lines of WikiMediatorServer, acting as a convenient
     * all-in-one test, that allows us to view the results on the console (currently commented out).
     *
     * Further, the way this is setup makes it Modular and changeable, each test can be altered
     * by either changing the Request object, and sending requests is made simple by the setup work
     * done at the beginning.
     */
    public static void main(String[] args) {
        try {

            Gson gson = new Gson();

            // SEARCH
            JsonObject reqObjSearch = new JsonObject();
            reqObjSearch.addProperty("id","1");
            reqObjSearch.addProperty("type","search");
            reqObjSearch.addProperty("query","Sport");
            reqObjSearch.addProperty("limit","3");

            final String searchReq = gson.toJson(reqObjSearch);

            reqObjSearch.addProperty("timeout","1");
            final String searchReqT = gson.toJson(reqObjSearch);

            // GETPAGE

            JsonObject reqObjGetPage = new JsonObject();
            reqObjGetPage.addProperty("id","2");
            reqObjGetPage.addProperty("type","getPage");
            reqObjGetPage.addProperty("pageTitle","Barack Obama");

            final String getPageReq = gson.toJson(reqObjGetPage);

            reqObjGetPage.addProperty("timeout","5");
            final String getPageReqT = gson.toJson(reqObjGetPage);

            // ZEITGEIST

            JsonObject reqObjZeit = new JsonObject();
            reqObjZeit.addProperty("id","3");
            reqObjZeit.addProperty("type","zeitgeist");
            reqObjZeit.addProperty("limit","3");

            final String zeitReq = gson.toJson(reqObjZeit);

            reqObjZeit.addProperty("timeout","5");
            final String zeitReqT = gson.toJson(reqObjZeit);

            //TRENDING
            JsonObject reqObjTrend = new JsonObject();
            reqObjTrend.addProperty("id","4");
            reqObjTrend.addProperty("type","trending");
            reqObjTrend.addProperty("timeLimitInSeconds","3");
            reqObjTrend.addProperty("maxItems","3");

            final String trendReq = gson.toJson(reqObjTrend);

            reqObjTrend.addProperty("timeout","5");
            final String trendReqT = gson.toJson(reqObjTrend);


            //WINDOWED WPL
            JsonObject reqObjWPL = new JsonObject();
            reqObjWPL.addProperty("id","5");
            reqObjWPL.addProperty("type","windowedPeakLoad");
            reqObjWPL.addProperty("timeWindowInSeconds","3");

            final String wplReq = gson.toJson(reqObjWPL);

            reqObjWPL.addProperty("timeout","5");
            final String wplReqT = gson.toJson(reqObjWPL);


            //NON-WINDOWED WPL
            JsonObject reqObjNWPL = new JsonObject();
            reqObjNWPL.addProperty("id","6");
            reqObjNWPL.addProperty("type","windowedPeakLoad");

            final String nwplReq = gson.toJson(reqObjNWPL);

            reqObjNWPL.addProperty("timeout","5");
            final String nwplReqT = gson.toJson(reqObjNWPL);

            //STOP REQUEST
            JsonObject reqObjStop = new JsonObject();
            reqObjStop.addProperty("id","7");
            reqObjStop.addProperty("type","stop");

            final String stopReq = gson.toJson(reqObjStop);

            reqObjStop.addProperty("timeout","5");
            final String stopReqT = gson.toJson(reqObjStop);

            //SHORTEST PATH REQUEST
            JsonObject reqObjSP = new JsonObject();
            reqObjSP.addProperty("id","8");
            reqObjSP.addProperty("type","shortestPath");
            reqObjSP.addProperty("pageTitle1","BC Wildfire Service");
            reqObjSP.addProperty("pageTitle2","Abbotsford, British Columbia");
            reqObjSP.addProperty("timeout","30");

            final String spReq = gson.toJson(reqObjSP);


            for(int i=1;i<6;i++){

                WikiMediatorClient client = new WikiMediatorClient("127.0.0.1",
                    WikiMediatorServer.SERVER_PORT,i);

                Thread requester = new Thread(new Runnable() {

                    public void run() {

                        if(client.clientID != 3){
                            try {
                                int numRequests = 13;
                                client.sendRequest(searchReq);
                                client.sendRequest(getPageReq);
                                client.sendRequest(zeitReq);
                                client.sendRequest(trendReq);
                                client.sendRequest(nwplReq);
                                client.sendRequest(wplReq);
                                client.sendRequest(spReq);
                                client.sendRequest(searchReqT);
                                client.sendRequest(getPageReqT);
                                client.sendRequest(zeitReqT);
                                client.sendRequest(trendReqT);
                                client.sendRequest(nwplReqT);
                                client.sendRequest(wplReqT);

                                for(int i=0;i<numRequests;i++){

                                    String reply = client.getReply();

                                    // below statement was previously used for testing, now removed
                                    // to save your console from literally containing multiple entire
                                    // Wikipedia articles :)
                                    //System.out.println("Reply to client "+client.clientID+": " + reply);

                                }
                                client.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{
                            try {
                                client.sendRequest(stopReq);
                                String reply = client.getReply();
                                client.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }

                    }
                });
                // start the thread
                requester.start();
            }

        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Send a request to the server. Requires this is "open".
     *
     * @param request to find using WikiMediator methods, formatted as a Json.
     * @throws IOException if network or server failure
     */

    public void sendRequest(String request) throws IOException {
        out.print(request + "\n");
        out.flush(); // important! make sure request actually gets sent
    }

    /**
     * Get a reply from the next request that was submitted.
     * Requires this is "open".
     *
     * @return the requested Fibonacci number
     * @throws IOException if network or server failure
     */
    public String getReply() throws IOException {
        String reply = in.readLine();
        if (reply == null) {
            throw new IOException("connection terminated unexpectedly");
        }

        try {
            return new String(reply);
        }
        catch (NumberFormatException nfe) {
            throw new IOException("misformatted reply: " + reply);
        }
    }

    /**
     * Closes the client's connection to the server.
     * This client is now "closed". Requires this is "open".
     *
     * @throws IOException if close fails
     */
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

}
