
package it.greenvulcano.gvesb.channel.mongodb.exception;

public class PropertyNotFoundException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 4041513500012144434L;

	public PropertyNotFoundException() {

        super("Property not found in property file(s)");

    }

    public PropertyNotFoundException(String propertyKey) {

        super("Property " + propertyKey + " not found in the properties file(s)");

    }

}
