package org.springframework.integration.aggregator;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;

public class MethodInvokingAggregatorFactoryBean implements
		FactoryBean<BufferingMessageHandler> , InitializingBean{

	private static final int DEFAULT_CAPACITY = Integer.MAX_VALUE;

	private MessageStore store = new SimpleMessageStore(DEFAULT_CAPACITY);

	private CorrelationStrategy correlationStrategy = new HeaderAttributeCorrelationStrategy(
			MessageHeaders.CORRELATION_ID);

	private CompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();

	private MessagesProcessor processor;
	
	private Object target;
	
	public void setTarget(Object target) {
		this.target = target;
	}
	
	public void afterPropertiesSet() throws Exception {
		// build correllation strategy
		// build completion strategy
		// build processor
	}

	public BufferingMessageHandler getObject() throws Exception {
		return new BufferingMessageHandler(store, correlationStrategy,
				completionStrategy, processor);
	}

	public Class<? extends BufferingMessageHandler> getObjectType() {
		return BufferingMessageHandler.class;
	}

	public boolean isSingleton() {
		return false;
	}
}
