/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.transformer;

import java.util.Collection;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A reply-producing {@link org.springframework.messaging.MessageHandler}
 * that delegates to a {@link Transformer} instance to modify the received {@link Message}
 * and sends the result to its output channel.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
public class MessageTransformingHandler extends AbstractReplyProducingMessageHandler implements Lifecycle {

	private final Transformer transformer;

	/**
	 * Create a {@link MessageTransformingHandler} instance that delegates to
	 * the provided {@link Transformer}.
	 * @param transformer The transformer.
	 */
	public MessageTransformingHandler(Transformer transformer) {
		Assert.notNull(transformer, "transformer must not be null");
		this.transformer = transformer;
		this.setRequiresReply(true);
	}


	@Override
	public String getComponentType() {
		return (this.transformer instanceof NamedComponent)
				? ((NamedComponent) this.transformer).getComponentType()
				: "transformer";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return (this.transformer instanceof IntegrationPattern)
				? ((IntegrationPattern) this.transformer).getIntegrationPatternType()
				: IntegrationPatternType.transformer;
	}

	@Override
	public void addNotPropagatedHeaders(String... headers) {
		super.addNotPropagatedHeaders(headers);
		populateNotPropagatedHeadersIfAny();
	}

	@Override
	protected void doInit() {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null && this.transformer instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.transformer).setBeanFactory(beanFactory);
		}

		populateNotPropagatedHeadersIfAny();
	}

	private void populateNotPropagatedHeadersIfAny() {
		Collection<String> notPropagatedHeaders = getNotPropagatedHeaders();

		if (this.transformer instanceof AbstractMessageProcessingTransformer && !notPropagatedHeaders.isEmpty()) {
			((AbstractMessageProcessingTransformer) this.transformer)
					.setNotPropagatedHeaders(notPropagatedHeaders.toArray(new String[0]));
		}
	}

	@Override
	public void start() {
		if (this.transformer instanceof Lifecycle) {
			((Lifecycle) this.transformer).start();
		}
	}

	@Override
	public void stop() {
		if (this.transformer instanceof Lifecycle) {
			((Lifecycle) this.transformer).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.transformer instanceof Lifecycle) || ((Lifecycle) this.transformer).isRunning();
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		try {
			return this.transformer.transform(message);
		}
		catch (Exception e) {
			if (e instanceof MessageTransformationException) { // NOSONAR
				throw (MessageTransformationException) e;
			}
			throw new MessageTransformationException(message, "Failed to transform Message in " + this, e);
		}
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

}
