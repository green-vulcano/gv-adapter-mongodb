package it.greenvulcano.gvesb.channel.mongodb.exception;

public class QueryFailedException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8753999589907305835L;

	public QueryFailedException() {

        super("Query to MongoDB failed");

    }

    public QueryFailedException(String message) {

        super("Query to MongoDB failed: " + message);

    }

}
