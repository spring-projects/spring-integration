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
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

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
 */
public class RecipientListRouter extends AbstractMessageRouter implements InitializingBean {

	private volatile List<Recipient> recipients;


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
		this.recipients = recipients;
	}

	@Override
	public String getComponentType() {
		return "recipient-list-router";
	}

	@Override
	public void onInit() throws Exception {
		Assert.notEmpty(this.recipients, "recipient list must not be empty");
		super.onInit();
	}

	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		List<Recipient> recipientList = this.recipients;
		for (Recipient recipient : recipientList) {
			if (recipient.accept(message)) {
				channels.add(recipient.getChannel());
			}
		}
		return channels;
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


		public MessageChannel getChannel() {
			return this.channel;
		}

		public boolean accept(Message<?> message) {
			return (this.selector == null || this.selector.accept(message));
		}
	}

}
