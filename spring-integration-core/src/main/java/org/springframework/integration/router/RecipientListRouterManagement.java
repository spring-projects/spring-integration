/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.router;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.springframework.integration.router.RecipientListRouter.Recipient;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Exposes adding/removing individual recipients operations for
 * RecipientListRouter. This can be used with a control-bus and JMX.
 *
 * @author Liujiong
 * @since 4.1
 *
 */
@ManagedResource
public interface RecipientListRouterManagement {

	/**
	 * Add a recipient with channelName and expression.
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
	 */
	@ManagedOperation
	int removeRecipient(String channelName);

	/**
	 * Remove all recipients that match the channelName and expression.
	 * @param channelName The channel name.
	 * @param selectorExpression The expression to filter the incoming message
	 */
	@ManagedOperation
	int removeRecipient(String channelName, String selectorExpression);

	/**
	 * @return an unmodifiable collection of recipients.
	 */
	@ManagedAttribute
	Collection<Recipient> getRecipients();

	/**
	 * Replace recipient.
	 * @param recipientMappings contain channelName and expression.
	 */
	@ManagedOperation
	void replaceRecipients(Properties recipientMappings);

	/**
	 * Set recipients.
	 * @param recipientMappings contain channelName and expression.
	 */
	@ManagedAttribute
	void setRecipientMappings(Map<String, String> recipientMappings);

}
