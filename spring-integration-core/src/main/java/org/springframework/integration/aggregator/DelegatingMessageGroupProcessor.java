/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageGroupProcessor} implementation with delegation to the provided {@code delegate}
 * and optional aggregation for headers.
 * <p>
 * Unlike {@link AbstractAggregatingMessageGroupProcessor} this processor checks a result
 * of the {@code delegate} call and aggregates headers into the output only
 * if the result is not a {@link Message} or {@link AbstractIntegrationMessageBuilder}.
 * <p>
 * This processor is used internally for wrapping provided non-standard {@link MessageGroupProcessor}
 * when a aggregate headers {@link Function} is provided.
 * For POJO method invoking or SpEL expression evaluation it is recommended to use an
 * {@link AbstractAggregatingMessageGroupProcessor} implementations.
 *
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class DelegatingMessageGroupProcessor implements MessageGroupProcessor, BeanFactoryAware,
		ManageableLifecycle {

	private final MessageGroupProcessor delegate;

	private final Function<MessageGroup, Map<String, Object>> headersFunction;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile boolean messageBuilderFactorySet;

	private BeanFactory beanFactory;

	public DelegatingMessageGroupProcessor(MessageGroupProcessor delegate,
			Function<MessageGroup, Map<String, Object>> headersFunction) {

		Assert.notNull(delegate, "'delegate' must not be null");
		Assert.notNull(headersFunction, "'headersFunction' must not be null");
		this.delegate = delegate;
		this.headersFunction = headersFunction;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		if (this.delegate instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.delegate).setBeanFactory(beanFactory);
		}
	}

	@Override
	public Object processMessageGroup(MessageGroup group) {
		Object result = this.delegate.processMessageGroup(group);
		if (!(result instanceof Message<?>) && !(result instanceof AbstractIntegrationMessageBuilder)) {
			result = getMessageBuilderFactory()
					.withPayload(result)
					.copyHeadersIfAbsent(this.headersFunction.apply(group));
		}
		return result;
	}

	private MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	public void start() {
		if (this.delegate instanceof Lifecycle) {
			((Lifecycle) this.delegate).start();
		}
	}

	@Override
	public void stop() {
		if (this.delegate instanceof Lifecycle) {
			((Lifecycle) this.delegate).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.delegate instanceof Lifecycle && ((Lifecycle) this.delegate).isRunning();
	}

}
