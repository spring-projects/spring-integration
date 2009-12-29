package org.springframework.integration.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;

import static org.mockito.Mockito.*;

public class CorrelatingMessageHandlerIntegrationTest {

	private CompletionStrategy completionStrategy;
	private CorrelationStrategy correlationStrategy;
	private MessageStore store = new SimpleMessageStore(100);
	private MessageChannel outputChannel = mock(MessageChannel.class);
	private MessageGroupProcessor processor = new PassThroughMessageGroupProcessor();
	private CorrelatingMessageHandler defaultHandler = new CorrelatingMessageHandler(
			store, processor);

    @Before public void setupHandler(){
        defaultHandler.setOutputChannel(outputChannel);
    }

	private Message<?> correlatedMessage(Object correlationId,
			Integer sequenceSize, Integer sequenceNumber) {
		return MessageBuilder.withPayload("test").setCorrelationId(
				correlationId).setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize).build();
	}

	@Test
	public void completesSingleMessage() throws Exception {
		Message<?> message = correlatedMessage(1,
				1, 1);
		defaultHandler.handleMessage(message);
		verify(outputChannel).send(message);
	}
	
	@Test
	public void completesAfterSequenceComplete() throws Exception {
		Message<?> message1 = correlatedMessage(1, 2, 1);
		Message<?> message2 = correlatedMessage(1, 2, 2);
		defaultHandler.handleMessage(message1);
		verify(outputChannel, never()).send(message1);
		defaultHandler.handleMessage(message2);
		verify(outputChannel).send(message1);
		verify(outputChannel).send(message2);
	}
	
	@Test
	public void completesWithoutReleasingIncompleteCorrelations() throws Exception {
		Message<?> message1 = correlatedMessage(1, 2, 1);
		Message<?> message2 = correlatedMessage(2, 2, 1);
		Message<?> message1a = correlatedMessage(1, 2, 2);
		Message<?> message2a = correlatedMessage(2, 2, 2);
		defaultHandler.handleMessage(message1);
		defaultHandler.handleMessage(message2);
		verify(outputChannel, never()).send(message1);
		verify(outputChannel, never()).send(message2);
		defaultHandler.handleMessage(message1a);
		verify(outputChannel).send(message1);
		verify(outputChannel).send(message1a);
		verify(outputChannel, never()).send(message2);
		verify(outputChannel, never()).send(message2a);
		defaultHandler.handleMessage(message2a);
		verify(outputChannel).send(message2);
		verify(outputChannel).send(message2a);
	}
}
