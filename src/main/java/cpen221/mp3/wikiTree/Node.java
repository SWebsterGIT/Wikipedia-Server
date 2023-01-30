package cpen221.mp3.wikiTree;

import cpen221.mp3.wikimediator.WikiMediator;
import org.fastily.jwiki.core.MQuery;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.Wiki;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class Node implements Comparable<Node>{

    //Node

    //Abstraction Function
    /*
    A Node is a recursive data type which is inherently connected to other nodes by each node being either parents or
    children of other nodes, or both. Each node represents a single Wikipedia page, where the String "pageName" is the
    name of the wikipedia page. Each node also has a set of Nodes called "children" and a parent Node, which is the
    node that that node was produced from as a result of the links on the parent's page, and stored in the "parent"
    field of a node. Together, a network of connected nodes represents a tree of links originating from a Wikipedia
    page. If a node is the first node in a tree, its parent Node will be simply null.

    Because the Node object is used to search for a destination node, every node carries with itself the destination
    wikipedia page name which is being searched for, which is stored in the String "destination". To optimize
    performance and prevent infinite recursion, all nodes in a network share a list of pages which have already been
    searched, called "alreadySearched". Finally, each node carries with itself a "ConditionSet"*, which is an object
    responsible for keeping track of the current search progress such that if a timeout is reached, or the destination
    is found, the search will terminate in an orderly fashion. Finally, each Node carries a Wiki object in order to
    facilitate requests to retrieve links on Wikipedia articles.

    *See ConditionSet class for more details on ConditionSet.
     */

    //Representation Invariants
    /*
        1. If a tree-build is in progress (search), a node's ConditionSet must be valid. (Checked in method)
        2. A node's pageName must be the case-sensitive Wikipedia page title that the node represents.
        3. If a node is not a terminal node in a tree, it's children.size() must be greater than zero.
        4. If a node is the first node in a tree, it must have null as its parent node.
        5. A node may not have itself inside its set of children.
     */

    //Thread-Safety Arguments
    /*
    The node data type is not meant to be a datatype that is meant for concurrent use in nature, however is protected
    against multi-threading problems. The first measure against threading errors is that every field is private and
    final, however, the objects in those fields are still susceptible to mutation by the methods inside Node. To
    prevent any errors, these mutator methods have been made into synchronized methods.
     */

    private synchronized void checkRep(){
        assert(wiki.exists(this.pageName));
        if(this.children.size() > 0){
            if(this.children.contains(this)){
                assert(false);
            } else {
                assert(true);
            }
        }
    }

    private final Node parent;
    private final String pageName;
    private final Set<Node> children;
    private final Set<String> alreadySearched;
    private final String destination;
    private final ConditionSet conditionSet;
    private final static Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();

    /**
     * Constructor for the first Node in a network.
     * @param pageName      The case-sensitive page name of an existing Wikipedia page for which the
     *                      Node represents.
     * @param destination   The case-sensitive page name of an existing Wikipedia page for which the Node may
     *                      search for in a larger network.
     * @param timeout       The time value in seconds for which the node may search for a Wikipedia page before
     *                      a TimeoutException is thrown in the buildTree method.
     */
    public Node(String pageName, String destination, long timeout){
        this.pageName = pageName;
        this.children = new TreeSet<>();
        this.alreadySearched = new TreeSet<>();
        this.destination = destination;
        this.conditionSet = new ConditionSet(timeout);
        this.parent = null;
    }

    /**
     * Constructor for a child node.
     * @param pageName          The case-sensitive page name of an existing Wikipedia page for which the
     *                          Node represents.
     * @param destination       The case-sensitive page name of an existing Wikipedia page for which the parent Node
     *                          may search for if buildTree is called.
     * @param alreadySearched   The set of Wikipedia pages which have already been searched by the parent of this Node.
     * @param conditionSet      The conditionSet of the parent node.
     * @param parent            The parent Node of the node being created.
     */
    public Node(String pageName, String destination, Set<String> alreadySearched,
                ConditionSet conditionSet, Node parent){
        this.pageName = pageName;
        this.children = new TreeSet<>();
        this.alreadySearched = alreadySearched;
        this.destination = destination;
        this.conditionSet = conditionSet;
        //Checks to see if this node is the destination node, terminates conditionSet if true.
        this.parent = parent;
        if(this.pageName.equals(this.destination)){
            this.conditionSet.terminate();
        }
    }

    /**
     * Main search method for a Node. Builds the tree of Wikipedia pages from the current node until the destination
     * node is found, otherwise throws a TimeoutException.
     * @throws TimeoutException     if the tree-building process takes longer than the specified timeout value in the
     *                              origin node.
     */
    public synchronized void buildTree() throws TimeoutException{

        //checks to see that the origin node is not the destination node.
        if(!this.pageName.equals(this.destination)){

            //initializes the first node
            this.generateChildren();

            Queue<Node> queue = new LinkedList<>();

            queue.addAll(this.children);

            boolean found = false;

            //Continues building layers and nodes until the destination is found.
            while(!found){
                //Pops the first node in the queue.
                Node current = queue.remove();

                //Checks if the current node is the destination node.
                if(current.pageName.equals(this.destination)){
                    found = true;
                    break;
                } else {
                    //Builds the children for the current node and adds them to the end of the queue to search next.
                    current.generateChildren();
                    queue.addAll(current.children);
                }
            }
        }
    }

    /**
     * Helper method for buildTree. Generates the children nodes for the current instance of Node. Requires that
     * the current instance of Node has links on its page. Does not generate children nodes if they have already
     * been generated in previous nodes.
     * @throws TimeoutException if the Node which is being expanded has exceeded its timeout specified at construction.
     */
    private synchronized void generateChildren() throws TimeoutException {
        if(this.conditionSet.check()){
            //gets all of the new links from the page
            Set<String> links = (wiki.getLinksOnPage(this.pageName)).parallelStream()
                    .filter(x -> !this.alreadySearched.contains(x)).distinct()
                    .collect(Collectors.toCollection(TreeSet::new));

            this.alreadySearched.addAll(links);

            //creates new children nodes.
            this.children.addAll(links.parallelStream()
                    .map(x -> new Node(x, this.destination, this.alreadySearched, this.conditionSet, this))
                    .collect(Collectors.toSet()));
        }
    }

    /**
     * Retrieves the destination path from the current node to its destination node. Requires that the current instance
     * of Node that getDestinationPath is called from is the origin Node of a node network/tree.
     * @return  The shortest, lexicographically smallest path of nodes from the origin Node to
     *          reach its destination Node.
     */
    public synchronized List<String> getDestinationPath(){

        List<String> destinationPath = new LinkedList<>();
        Node destination = this.findDestination();

        destinationPath.add(destination.pageName);

        //traces every node's parents until the origin is reached.
        while(destination.parent != null){
            destination = destination.parent;
            destinationPath.add(destination.pageName);
        }
        Collections.reverse(destinationPath);
        return destinationPath;
    }

    /**
     * Returns the Node object corresponding to the destination of the current instance of the node.
     * @return  the Node object corresponding to the destination Node of the current instance of Node.
     * @throws NoSuchElementException   if the destination node is not a direct nor indirect child of the current node,
     *                                  or if the destination node does not exist in the tree. "Indirect child" is if
     *                                  a node is the child of a child (of another child, etc) of the current node.
     */
    private synchronized Node findDestination() throws NoSuchElementException{

        if(this.pageName.equals(this.destination)){
            return this;
        } else {
            Queue<Node> queue = new LinkedList<>();
            queue.add(this);

            boolean found = false;

            //breadth-first search of the tree.
            while(!found){
                Node temp = queue.remove();
                if(temp.pageName.equals(this.destination)){
                    found = true;
                    return temp;
                } else {
                    queue.addAll(temp.children);
                }
            }
            throw new NoSuchElementException();
        }

    }

    /**
     *
     * @param o the Node to compare the current instance of Node to.
     * @return  1 if the pageName of o is lexiographically larger than the pageName of the current instance of Node, 0
     *          if they are the same, and -1 if it is lesser.
     */
    @Override
    public int compareTo(@NotNull Node o) {
        if(!(o instanceof Node)){
            return 0;
        }
        return this.pageName.compareTo(((Node) o).pageName);
    }
}
