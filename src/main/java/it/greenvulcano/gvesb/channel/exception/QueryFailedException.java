package it.greenvulcano.gvesb.channel.exception;

public class QueryFailedException extends Exception {

    public QueryFailedException() {

        super("Query to MongoDB failed");

    }

    public QueryFailedException(String message) {

        super("Query to MongoDB failed: " + message);

    }

}
