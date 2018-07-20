package it.greenvulcano.gvesb.channel.mongodb.exception;

public class PropertyInitializationException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8329171790614338236L;

	public PropertyInitializationException() {

        super("Properties component could not be initialized successfully");

    }

    public PropertyInitializationException(String message) {

        super("Properties component could not be initialized successfully: " + message);

    }

}
