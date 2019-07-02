/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Base class for MessageGroupProcessor implementations that aggregate the group of Messages into a single Message.
 *
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractAggregatingMessageGroupProcessor implements MessageGroupProcessor,
		BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR - final

	private Function<MessageGroup, Map<String, Object>> headersFunction = new DefaultAggregateHeadersFunction();

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactorySet;

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Specify a {@link Function} to map {@link MessageGroup} into composed headers for output message.
	 * @param headersFunction the {@link Function} to use.
	 * @since 5.2
	 */
	public void setHeadersFunction(Function<MessageGroup, Map<String, Object>> headersFunction) {
		Assert.notNull(headersFunction, "'headersFunction' must not be null");
		this.headersFunction = headersFunction;
	}

	protected Function<MessageGroup, Map<String, Object>> getHeadersFunction() {
		return this.headersFunction;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	public final Object processMessageGroup(MessageGroup group) {
		Assert.notNull(group, "MessageGroup must not be null");
		Map<String, Object> headers = aggregateHeaders(group);
		Object payload = aggregatePayloads(group, headers);
		AbstractIntegrationMessageBuilder<?> builder;
		if (payload instanceof Message<?>) {
			builder = getMessageBuilderFactory().fromMessage((Message<?>) payload);
		}
		else if (payload instanceof AbstractIntegrationMessageBuilder) {
			builder = (AbstractIntegrationMessageBuilder<?>) payload;
		}
		else {
			builder = getMessageBuilderFactory().withPayload(payload);
		}

		return builder.copyHeadersIfAbsent(headers);
	}

	/**
	 * This default implementation simply returns all headers that have no conflicts among the group. An absent header
	 * on one or more Messages within the group is not considered a conflict. Subclasses may override this method with
	 * more advanced conflict-resolution strategies if necessary.
	 * @param group The message group.
	 * @return The aggregated headers.
	 */
	protected Map<String, Object> aggregateHeaders(MessageGroup group) {
		return getHeadersFunction().apply(group);
	}

	protected abstract Object aggregatePayloads(MessageGroup group, Map<String, Object> defaultHeaders);

}
