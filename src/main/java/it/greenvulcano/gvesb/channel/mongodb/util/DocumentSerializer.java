package it.greenvulcano.gvesb.channel.mongodb.util;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bson.Document;

import java.io.IOException;

public class DocumentSerializer extends StdSerializer<Document> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3037320554273960620L;

	public DocumentSerializer() { this(null); }

    public DocumentSerializer(Class<Document> aClass) { super(aClass); }

    @Override
    public void serialize(Document document, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonGenerationException {

        jsonGenerator.writeString(document.toJson());

    }

}
