package org.springframework.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.MessageMatcher;
import org.springframework.integration.support.MessageBuilder;


public class JsonSymmetricalMessageMappingTests {

	private final JsonFactory jsonFactory = new JsonFactory();

	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@Factory
    public static Matcher<Message<?>> sameExceptImmutableHeaders(Message<?> operand) {
        return new MessageMatcher(operand);
    }
	
	@Test
	public void testSymmetricalMappingWithHistory() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(1));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(2));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(3));
		JsonOutboundMessageMapper outboundMapper = new JsonOutboundMessageMapper();
		
		String outboundJson = outboundMapper.fromMessage(testMessage);
		System.out.println(outboundJson);
		
		JsonInboundMessageMapper inboundMapper = new JsonInboundMessageMapper(String.class);
		Message<String> result = (Message<String>) inboundMapper.toMessage(outboundJson);
		
		assertThat(result, sameExceptImmutableHeaders(testMessage));
		//assertEquals(testMessage, result);
		
		outboundJson = outboundMapper.fromMessage(result);
		System.out.println(outboundJson);
	}
	
	private static class TestNamedComponent implements NamedComponent {

		private final int id;

		private TestNamedComponent(int id) {
			this.id = id;
		}

		public String getComponentName() {
			return "testName-" + this.id;
		}

		public String getComponentType() {
			return "testType-" + this.id;
		}

	}
}
