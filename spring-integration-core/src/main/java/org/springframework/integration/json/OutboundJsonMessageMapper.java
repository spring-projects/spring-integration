package org.springframework.integration.json;

import java.io.StringWriter;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.integration.Message;
import org.springframework.integration.mapping.OutboundMessageMapper;

/**
 * {@link OutboundMessageMapper} implementation the converts a {@link Message} to a JSON string representation.
 * 
 * TODO - We might need to add special handling for MessageHistory
 * 
 * @author Jeremy Grelle
 */
public class OutboundJsonMessageMapper implements OutboundMessageMapper<String> {

	private boolean shouldExtractPayload = false;
	
	private ObjectMapper objectMapper = new ObjectMapper();
	
	public String fromMessage(Message<?> message) throws Exception {
		StringWriter writer = new StringWriter();
		if (shouldExtractPayload) {
			objectMapper.writeValue(writer, message.getPayload());
		} else {
			objectMapper.writeValue(writer, message);
		}
		return writer.toString();
	}
	
	public void setShouldExtractPayload(boolean shouldExtractPayload) {
		this.shouldExtractPayload = shouldExtractPayload;
	}
}
