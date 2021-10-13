/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
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
 * @author Gary Russell
 */
public class RecipientListRouter extends AbstractMessageRouter implements RecipientListRouterManagement {

	private volatile Queue<Recipient> recipients = new ConcurrentLinkedQueue<>();

	/**
	 * Set the channels for this router. Either call this method or
	 * {@link #setRecipients(List)} but not both. If MessageSelectors should be
	 * considered, then use {@link #setRecipients(List)}.
	 * @param channels The channels.
	 */
	public void setChannels(List<MessageChannel> channels) {
		Assert.notEmpty(channels, "'channels' must not be empty");
		setRecipients(channels.stream()
				.map(Recipient::new)
				.collect(Collectors.toList()));
	}

	/**
	 * Set the recipients for this router.
	 * @param recipients The recipients.
	 */
	public void setRecipients(List<Recipient> recipients) {
		Assert.notEmpty(recipients, "'recipients' must not be empty");
		Queue<Recipient> newRecipients = new ConcurrentLinkedQueue<>(recipients);
		newRecipients.forEach(this::setupRecipient);
		logger.debug(() -> "Channel Recipients: " + this.recipients + " replaced with: " + newRecipients);
		this.recipients = newRecipients;
	}

	/**
	 * Set the recipients for this router.
	 * @param recipientMappings map contains channelName and expression
	 */
	@Override
	@ManagedAttribute
	public void setRecipientMappings(Map<String, String> recipientMappings) {
		Assert.notEmpty(recipientMappings, "'recipientMappings' must not be empty");
		Assert.noNullElements(recipientMappings.keySet().toArray(), "'recipientMappings' cannot have null keys.");
		Queue<Recipient> newRecipients = new ConcurrentLinkedQueue<>();
		for (Entry<String, String> next : recipientMappings.entrySet()) {
			if (StringUtils.hasText(next.getValue())) {
				addRecipient(next.getKey(), next.getValue(), newRecipients);
			}
			else {
				addRecipient(next.getKey(), (MessageSelector) null, newRecipients);
			}
		}
		logger.debug(() -> "Channel Recipients: " + this.recipients + " replaced with: " + newRecipients);
		this.recipients = newRecipients;
	}

	@Override
	@ManagedOperation
	public void addRecipient(String channelName, String selectorExpression) {
		addRecipient(channelName, selectorExpression, this.recipients);
	}

	private void addRecipient(String channelName, String selectorExpression, Queue<Recipient> recipientsToAdd) {
		Assert.hasText(channelName, "'channelName' must not be empty.");
		Assert.hasText(selectorExpression, "'selectorExpression' must not be empty.");
		ExpressionEvaluatingSelector expressionEvaluatingSelector =
				new ExpressionEvaluatingSelector(selectorExpression);
		expressionEvaluatingSelector.setBeanFactory(getBeanFactory());
		Recipient recipient = new Recipient(channelName, expressionEvaluatingSelector);
		setupRecipient(recipient);
		recipientsToAdd.add(recipient);
	}

	@Override
	@ManagedOperation
	public void addRecipient(String channelName) {
		addRecipient(channelName, (MessageSelector) null);
	}

	public void addRecipient(String channelName, MessageSelector selector) {
		addRecipient(channelName, selector, this.recipients);
	}

	private void addRecipient(String channelName, MessageSelector selector, Queue<Recipient> recipientsToAdd) {
		Assert.hasText(channelName, "'channelName' must not be empty.");
		Recipient recipient = new Recipient(channelName, selector);
		setupRecipient(recipient);
		recipientsToAdd.add(recipient);
	}

	public void addRecipient(MessageChannel channel) {
		addRecipient(channel, null);
	}

	public void addRecipient(MessageChannel channel, MessageSelector selector) {
		Recipient recipient = new Recipient(channel, selector);
		setupRecipient(recipient);
		this.recipients.add(recipient);
	}

	private void setupRecipient(Recipient recipient) {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			recipient.setChannelResolver(getChannelResolver());
			if (recipient.selector instanceof BeanFactoryAware) {
				((BeanFactoryAware) recipient.selector).setBeanFactory(beanFactory);
			}
		}
	}

	@Override
	@ManagedOperation
	public int removeRecipient(String channelName) {
		int counter = 0;
		MessageChannel channel = getChannelResolver().resolveDestination(channelName);
		for (Iterator<Recipient> it = this.recipients.iterator(); it.hasNext(); ) {
			if (channel.equals(it.next().getChannel())) {
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
		MessageChannel targetChannel = getChannelResolver().resolveDestination(channelName);
		for (Iterator<Recipient> it = this.recipients.iterator(); it.hasNext(); ) {
			Recipient next = it.next();
			MessageSelector selector = next.getSelector();
			MessageChannel channel = next.getChannel();
			if (selector instanceof ExpressionEvaluatingSelector
					&& targetChannel.equals(channel)
					&& ((ExpressionEvaluatingSelector) selector).getExpressionString().equals(selectorExpression)) {
				it.remove();
				counter++;
			}
		}
		return counter;
	}

	@Override
	@ManagedOperation
	public void replaceRecipients(Properties recipientMappings) {
		Assert.notEmpty(recipientMappings, "'recipientMappings' must not be empty");
		Set<String> keys = recipientMappings.stringPropertyNames();
		Queue<Recipient> originalRecipients = this.recipients;
		this.recipients.clear();
		for (String key : keys) {
			Assert.notNull(key, "channelName can't be null.");
			if (StringUtils.hasText(recipientMappings.getProperty(key))) {
				addRecipient(key, recipientMappings.getProperty(key));
			}
			else {
				addRecipient(key);
			}
		}
		logger.debug(() -> "Channel Recipients: " + originalRecipients + " replaced with: " + this.recipients);
	}

	@Override
	@ManagedAttribute
	public Collection<Recipient> getRecipients() {
		return Collections.unmodifiableCollection(this.recipients);
	}

	@Override
	public String getComponentType() {
		return "recipient-list-router";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.recipient_list_router;
	}

	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		List<MessageChannel> result = new ArrayList<>();
		for (Recipient recipient : this.recipients) {
			if (recipient.accept(message)) {
				result.add(recipient.getChannel());
			}
		}
		return result;
	}


	@Override
	protected void onInit() {
		super.onInit();
		this.recipients.forEach(this::setupRecipient);
	}

	public static class Recipient {

		private final MessageSelector selector;

		private MessageChannel channel;

		private String channelName;

		private DestinationResolver<MessageChannel> channelResolver;

		public Recipient(MessageChannel channel) {
			this(channel, null);
		}

		public Recipient(MessageChannel channel, MessageSelector selector) {
			this.channel = channel;
			this.selector = selector;
		}

		public Recipient(String channelName) {
			this(channelName, null);
		}

		public Recipient(String channelName, MessageSelector selector) {
			this.channelName = channelName;
			this.selector = selector;
		}

		public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
			this.channelResolver = channelResolver;
		}

		private MessageSelector getSelector() {
			return this.selector;
		}

		@Nullable
		public MessageChannel getChannel() {
			if (this.channel == null) {
				String channelNameForInitialization = this.channelName;
				if (channelNameForInitialization != null && this.channelResolver != null) {
					this.channel = this.channelResolver.resolveDestination(channelNameForInitialization);
					this.channelName = null;
				}
			}
			return this.channel;
		}

		public boolean accept(Message<?> message) {
			return (this.selector == null || this.selector.accept(message));
		}

	}

}
