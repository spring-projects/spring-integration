/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.router;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Exposes adding/removing individual recipients operations for
 * RecipientListRouter. This can be used with a control-bus and JMX.
 *
 * @author Liujiong
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 *
 */
@ManagedResource
@IntegrationManagedResource
public interface RecipientListRouterManagement {

	/**
	 * Add a recipient with channelName and expression.
	 * The expression follows only
	 * {@link org.springframework.expression.spel.support.SimpleEvaluationContext#forReadOnlyDataBinding()}
	 * capabilities. Otherwise, use non-managed {@link RecipientListRouter#addRecipient(String, MessageSelector)}
	 * API with more control over execution.
	 * @param channelName The channel name.
	 * @param selectorExpression The expression to filter the incoming message.
	 */
	@ManagedOperation
	void addRecipient(String channelName, String selectorExpression);

	/**
	 * Add a recipient with channelName.
	 * @param channelName The channel name.
	 */
	@ManagedOperation
	void addRecipient(String channelName);

	/**
	 * Remove all recipients that match the channelName.
	 * @param channelName The channel name.
	 * @return The number of recipients removed.
	 */
	@ManagedOperation
	int removeRecipient(String channelName);

	/**
	 * Remove all recipients that match the channelName and expression.
	 * @param channelName The channel name.
	 * @param selectorExpression The expression to filter the incoming message
	 * @return The number of recipients removed.
	 */
	@ManagedOperation
	int removeRecipient(String channelName, String selectorExpression);

	/**
	 * @return an unmodifiable collection of recipients.
	 */
	@ManagedAttribute
	Collection<?> getRecipients();

	/**
	 * Replace recipient.
	 * The expression follows only
	 * {@link org.springframework.expression.spel.support.SimpleEvaluationContext#forReadOnlyDataBinding()}
	 * capabilities. Otherwise, use non-managed {@link RecipientListRouter#addRecipient(String, MessageSelector)}
	 * API with more control over execution.
	 * @param recipientMappings contain channelName and expression.
	 */
	@ManagedOperation
	void replaceRecipients(Properties recipientMappings);

	/**
	 * Set recipients.
	 * The expression follows only
	 * {@link org.springframework.expression.spel.support.SimpleEvaluationContext#forReadOnlyDataBinding()}
	 * capabilities. Otherwise, use non-managed {@link RecipientListRouter#setRecipients(List)}
	 * API with more control over execution.
	 * @param recipientMappings contain channelName and expression.
	 */
	@ManagedAttribute
	void setRecipientMappings(Map<String, String> recipientMappings);

}
