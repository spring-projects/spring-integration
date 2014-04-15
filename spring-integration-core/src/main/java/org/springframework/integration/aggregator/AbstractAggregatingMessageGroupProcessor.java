/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * Base class for MessageGroupProcessor implementations that aggregate the group of Messages into a single Message.
 *
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @author Dave Syer
 * @since 2.0
 */
public abstract class AbstractAggregatingMessageGroupProcessor implements MessageGroupProcessor,
		BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(beanFactory);
	}

	@Override
	public final Object processMessageGroup(MessageGroup group) {
		Assert.notNull(group, "MessageGroup must not be null");

		Map<String, Object> headers = this.aggregateHeaders(group);
		Object payload = this.aggregatePayloads(group, headers);
		AbstractIntegrationMessageBuilder<?> builder;
		if (payload instanceof Message<?>) {
			builder = this.messageBuilderFactory.fromMessage((Message<?>) payload).copyHeadersIfAbsent(headers);
		}
		else {
			builder = this.messageBuilderFactory.withPayload(payload).copyHeadersIfAbsent(headers);
		}

		return builder.popSequenceDetails().build();
	}

	/**
	 * This default implementation simply returns all headers that have no conflicts among the group. An absent header
	 * on one or more Messages within the group is not considered a conflict. Subclasses may override this method with
	 * more advanced conflict-resolution strategies if necessary.
	 *
	 * @param group The message group.
	 * @return The aggregated headers.
	 */
	protected Map<String, Object> aggregateHeaders(MessageGroup group) {
		Map<String, Object> aggregatedHeaders = new HashMap<String, Object>();
		Set<String> conflictKeys = new HashSet<String>();
		for (Message<?> message : group.getMessages()) {
			MessageHeaders currentHeaders = message.getHeaders();
			for (String key : currentHeaders.keySet()) {
				if (MessageHeaders.ID.equals(key) || MessageHeaders.TIMESTAMP.equals(key)
						|| IntegrationMessageHeaderAccessor.SEQUENCE_SIZE.equals(key) || IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER.equals(key)) {
					continue;
				}
				Object value = currentHeaders.get(key);
				if (!aggregatedHeaders.containsKey(key)) {
					aggregatedHeaders.put(key, value);
				}
				else if (!value.equals(aggregatedHeaders.get(key))) {
					conflictKeys.add(key);
				}
			}
		}
		for (String keyToRemove : conflictKeys) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding header '" + keyToRemove + "' upon aggregation due to conflict(s) "
						+ "in MessageGroup with correlation key: " + group.getGroupId());
			}
			aggregatedHeaders.remove(keyToRemove);
		}
		return aggregatedHeaders;
	}

	protected abstract Object aggregatePayloads(MessageGroup group, Map<String, Object> defaultHeaders);

}
