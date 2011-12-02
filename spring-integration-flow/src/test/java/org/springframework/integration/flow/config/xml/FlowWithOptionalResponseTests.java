package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FlowWithOptionalResponseTests {
	@Autowired
	@Qualifier("inputC")
	MessageChannel input;

	@Autowired
	@Qualifier("inputCO")
	MessageChannel inputForOptionalResponse;

	@Autowired
	@Qualifier("outputC")
	SubscribableChannel output;

	@Test
	public void testOneWay() {

		input.send(new GenericMessage<String>("hello"));
	}

	@Test
	public void testOptionResponse() {
		TestMessageHandler counter = new TestMessageHandler();

		output.subscribe(counter);

		for (int i = 0; i < 100; i++) {
			inputForOptionalResponse.send(new GenericMessage<String>("hello"));
		}

		assertTrue(String.valueOf(counter.count), counter.count > 1 && counter.count < 100);
	}

	static class TestMessageHandler implements MessageHandler {
		int count = 0;

		public void handleMessage(Message<?> message) throws MessagingException {
			count++;
		}
	}

}
