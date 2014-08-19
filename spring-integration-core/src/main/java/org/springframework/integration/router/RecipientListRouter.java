/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <pre class="code">
 * {@code
 * <recipient-list-router id="simpleRouter" input-channel="routingChannelA">
 *     <recipient channel="channel1"/>
 *     <recipient channel="channel2"/>
 * </recipient-list-router>
 * }
 * </pre>
 * <p>
 * A Message Router that sends Messages to a list of recipient channels. The
 * recipients can be provided as a static list of {@link MessageChannel}
 * instances via the {@link #setChannels(List)} method, or for dynamic behavior,
 * the values can be provided via the {@link #setRecipients(List)} method.
 * <p>
 * For more advanced, programmatic control of dynamic recipient lists, consider
 * using the @Router annotation or extending {@link AbstractMappingMessageRouter} instead.
 * <p>
 * Contrary to a standard &lt;router .../&gt; this handler will try to send to
 * all channels that are configured as recipients. It is to channels what a
 * publish subscribe channel is to endpoints.
 * <p>
 * Using this class only makes sense if it is essential to send messages on
 * multiple channels instead of sending them to multiple handlers. If the latter
 * is an option using a publish subscribe channel is the more flexible solution.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Liujiong
 */
public class RecipientListRouter extends AbstractMessageRouter
		implements InitializingBean, RecipientListRouterManagement {

	private final ConcurrentLinkedQueue<Recipient> recipients = new ConcurrentLinkedQueue<Recipient>();

	/**
	 * Set the channels for this router. Either call this method or
	 * {@link #setRecipients(List)} but not both. If MessageSelectors should be
	 * considered, then use {@link #setRecipients(List)}.
	 * @param channels The channels.
	 */
	public void setChannels(List<MessageChannel> channels) {
		Assert.notEmpty(channels, "channels must not be empty");
		List<Recipient> recipients = new ArrayList<Recipient>();
		for (MessageChannel channel : channels) {
			recipients.add(new Recipient(channel));
		}
		this.setRecipients(recipients);
	}

	/**
	 * Set the recipients for this router.
	 * @param recipients The recipients.
	 */
	public void setRecipients(List<Recipient> recipients) {
		Assert.notEmpty(recipients, "recipients must not be empty");
		ConcurrentLinkedQueue<Recipient> originalRecipients = this.recipients;
		this.recipients.clear();
		this.recipients.addAll(recipients);
		if (logger.isDebugEnabled()) {
			logger.debug("Channel Recipients:" + originalRecipients + " replaced with:" + this.recipients);
		}
	}

	/**
	 * Set the recipients for this router.
	 * @param recipientMappings, map contains channelName and expression
	 */
	@Override
	@ManagedAttribute
	public void setRecipientMappings(Map<String, String> recipientMappings) {
		Assert.notEmpty(recipientMappings, "recipientMappings must not be empty");
		Assert.noNullElements(recipientMappings.keySet().toArray(), "'recipientMappings' cannot have null keys.");
		ConcurrentLinkedQueue<Recipient> originalRecipients = this.recipients;
		this.recipients.clear();
		for (Entry<String, String> next : recipientMappings.entrySet()) {
			if (StringUtils.hasText(next.getValue())) {
				this.addRecipient(next.getKey(), next.getValue());
			}
			else {
				this.addRecipient(next.getKey());
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Channel Recipients:" + originalRecipients + " replaced with:" + this.recipients);
		}
	}

	@Override
	public String getComponentType() {
		return "recipient-list-router";
	}


	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		for (Recipient recipient : this.recipients) {
			if (recipient.accept(message)) {
				channels.add(recipient.getChannel());
			}
		}
		return channels;
	}

	@Override
	@ManagedOperation
	public void addRecipient(String channelName, String selectorExpression) {
		Assert.hasText(channelName, "'channelName' must not be empty.");
		Assert.hasText(selectorExpression, "'selectorExpression' must not be empty.");
		MessageChannel channel = this.getBeanFactory().getBean(channelName, MessageChannel.class);
		ExpressionEvaluatingSelector expressionEvaluatingSelector = new ExpressionEvaluatingSelector(selectorExpression);
		expressionEvaluatingSelector.setBeanFactory(this.getBeanFactory());
		this.recipients.add(new Recipient(channel, expressionEvaluatingSelector));
	}

	@Override
	@ManagedOperation
	public void addRecipient(String channelName) {
		Assert.hasText(channelName, "'channelName' must not be empty.");
		MessageChannel channel = this.getBeanFactory().getBean(channelName, MessageChannel.class);
		this.recipients.add(new Recipient(channel));
	}

	@Override
	@ManagedOperation
	public int removeRecipient(String channelName) {
		int counter = 0;
		MessageChannel channel = this.getBeanFactory().getBean(channelName, MessageChannel.class);
		for (Iterator<Recipient> it = this.recipients.iterator(); it.hasNext(); ) {
			if (it.next().getChannel() == channel) {
				it.remove();
				counter++;
			}
		}
		return counter;
	}

	@Override
	@ManagedOperation
	public int removeRecipient(String channelName, String selectorExpression) {
		int counter = 0;
		MessageChannel targetChannel = this.getBeanFactory().getBean(channelName, MessageChannel.class);
		for (Iterator<Recipient> it = this.recipients.iterator(); it.hasNext(); ) {
			Recipient next = it.next();
			MessageSelector selector = next.getSelector();
			MessageChannel channel = next.getChannel();
			if (selector instanceof ExpressionEvaluatingSelector &&
					channel == targetChannel &&
					((ExpressionEvaluatingSelector) selector).getExpressionString().equals(selectorExpression)) {
				it.remove();
				counter++;
			}
		}
		return counter;
	}

	@Override
	@ManagedAttribute
	public Collection<Recipient> getRecipients() {
		return Collections.unmodifiableCollection(this.recipients);
	}

	@Override
	@ManagedOperation
	public void replaceRecipients(Properties recipientMappings) {
		Assert.notEmpty(recipientMappings, "'recipientMappings' must not be empty");
		Set<String> keys = recipientMappings.stringPropertyNames();
		ConcurrentLinkedQueue<Recipient> originalRecipients = this.recipients;
		this.recipients.clear();
		for (String key : keys) {
			Assert.notNull(key, "channelName can't be null.");
			if (StringUtils.hasText(recipientMappings.getProperty(key))) {
				this.addRecipient(key, recipientMappings.getProperty(key));
			}
			else {
				this.addRecipient(key);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Channel Recipients:" + originalRecipients	+ " replaced with:" + this.recipients);
		}
	}


	public static class Recipient {

		private final MessageChannel channel;

		private final MessageSelector selector;


		public Recipient(MessageChannel channel) {
			this(channel, null);
		}

		public Recipient(MessageChannel channel, MessageSelector selector) {
			this.channel = channel;
			this.selector = selector;
		}

		private MessageSelector getSelector() {
			return selector;
		}

		public MessageChannel getChannel() {
			return this.channel;
		}

		public boolean accept(Message<?> message) {
			return (this.selector == null || this.selector.accept(message));
		}

	}

}
