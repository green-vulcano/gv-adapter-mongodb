package it.greenvulcano.gvesb.channel.exception;

public class PropertyInitializationException extends Exception {

    public PropertyInitializationException() {

        super("Properties component could not be initialized successfully");

    }

    public PropertyInitializationException(String message) {

        super("Properties component could not be initialized successfully: " + message);

    }

}
