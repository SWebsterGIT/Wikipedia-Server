package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.Bufferable;

public class Page implements Bufferable {
    // TODO: Write RI and AF
    /*
    * Representation Invariant:
    * pageText is not empty
    * id is not empty
     */

    /*
    * Abstraction function:
    * Represents a wikipedia page with id of its page title and its contents
    * as a String
     */

    private String id;

    private String pageText;

    /**
     * Create a page with contents {@code pageText} to represent a
     * Wikipedia page
     * @param pageTitle title of wikipedia page
     * @param pageText contents of the page
     */
    public Page(String pageTitle, String pageText) {
        this.id = pageTitle;
        this.pageText = pageText;
    }

    /**
     * Get the text contained in a page
     * @return contents of a page
     */
    public String getText() {
        return pageText;
    }

    /**
     * Get the title of a page
     * @return the title of a page
     */
    public String id() {
        return id;
    }
}
