
package it.greenvulcano.gvesb.channel.exception;

public class PropertyNotFoundException extends Exception {

    public PropertyNotFoundException() {

        super("Property not found in property file(s)");

    }

    public PropertyNotFoundException(String propertyKey) {

        super("Property " + propertyKey + " not found in the properties file(s)");

    }

}
