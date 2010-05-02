/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * MessageHandler that holds a buffer of correlated messages in a MessageStore. This class takes care of correlated
 * groups of messages that can be completed in batches. It is useful for aggregating, resequencing, or custom
 * implementations requiring correlation.
 * <p/>
 * To customize this handler inject {@link CorrelationStrategy}, {@link ReleaseStrategy}, and
 * {@link MessageGroupProcessor} implementations as you require.
 * <p/>
 * By default the CorrelationStrategy will be a HeaderAttributeCorrelationStrategy and the ReleaseStrategy will be a
 * SequenceSizeReleaseStrategy.
 * 
 * @author Iwein Fuld
 * @author Dave Syer
 * @since 2.0
 */
public class CorrelatingMessageHandler extends AbstractMessageHandler implements MessageProducer {

	private static final Log logger = LogFactory.getLog(CorrelatingMessageHandler.class);

	public static final long DEFAULT_SEND_TIMEOUT = 1000L;

	public static final long DEFAULT_REAPER_INTERVAL = 1000L;

	public static final long DEFAULT_TIMEOUT = 60000L;

	private final MessageStore store;

	private final MessageGroupProcessor outputProcessor;

	private volatile CorrelationStrategy correlationStrategy = new HeaderAttributeCorrelationStrategy(
			MessageHeaders.CORRELATION_ID);

	private volatile ReleaseStrategy ReleaseStrategy = new SequenceSizeReleaseStrategy();

	private MessageChannel outputChannel;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();

	private volatile MessageChannel discardChannel = new NullChannel();

	private boolean sendPartialResultOnTimeout = false;

	private final ConcurrentMap<Object, Object> locks = new ConcurrentHashMap<Object, Object>();

	public CorrelatingMessageHandler(MessageStore store, CorrelationStrategy correlationStrategy,
			ReleaseStrategy ReleaseStrategy, MessageGroupProcessor processor) {
		Assert.notNull(store);
		Assert.notNull(processor);
		this.store = store;
		this.outputProcessor = processor;
		this.correlationStrategy = correlationStrategy == null ? new HeaderAttributeCorrelationStrategy(
				MessageHeaders.CORRELATION_ID) : correlationStrategy;
		this.ReleaseStrategy = ReleaseStrategy == null ? new SequenceSizeReleaseStrategy() : ReleaseStrategy;
		this.channelTemplate.setSendTimeout(DEFAULT_SEND_TIMEOUT);
	}

	public CorrelatingMessageHandler(MessageStore store, MessageGroupProcessor processor) {
		this(store, null, null, processor);
	}

	public CorrelatingMessageHandler(MessageGroupProcessor processor) {
		this(new SimpleMessageStore(0), new HeaderAttributeCorrelationStrategy(MessageHeaders.CORRELATION_ID),
				new SequenceSizeReleaseStrategy(), processor);
	}

	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy);
		this.correlationStrategy = correlationStrategy;
	}

	public void setReleaseStrategy(ReleaseStrategy ReleaseStrategy) {
		Assert.notNull(ReleaseStrategy);
		this.ReleaseStrategy = ReleaseStrategy;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		super.setTaskScheduler(taskScheduler);
	}

	public void setTimeout(long timeout) {
	}

	public void setReaperInterval(long reaperInterval) {
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "'outputChannel' must not be null");
		this.outputChannel = outputChannel;
	}

	public void setChannelResolver(ChannelResolver channelResolver) {
		super.setChannelResolver(channelResolver);
	}

	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.channelTemplate.setSendTimeout(sendTimeout);
	}

	public void setSendPartialResultOnTimeout(boolean sendPartialResultOnTimeout) {
		this.sendPartialResultOnTimeout = sendPartialResultOnTimeout;
	}

	@Override
	public String getComponentType() {
		return "aggregator";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {

		Object correlationKey = correlationStrategy.getCorrelationKey(message);
		if (logger.isDebugEnabled()) {
			logger.debug("Handling message with correlationKey [" + correlationKey + "]: " + message);
		}

		if (!correlationKey.equals(message.getHeaders().getCorrelationId())) {
			// TODO: strategise the treatment of overwritten correlation
			message = MessageBuilder.fromMessage(message).setCorrelationId(correlationKey).build();
		}

		Object lock = getLock(correlationKey);
		synchronized (lock) {

			Collection<Message<?>> messages = store.list(correlationKey);
			MessageGroup group = new MessageGroup(messages, correlationKey);

			if (group.add(message)) {
				// TODO: use try/catch to detect problem in group.add() and use
				// that to decide on discard?
				store(message, correlationKey);
				if (ReleaseStrategy.canRelease(group)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Completing group with correlationKey [" + correlationKey + "]");
					}
					outputProcessor.processAndSend(group, channelTemplate, this.resolveReplyChannel(message,
							this.outputChannel));
					if (group.isComplete()) {
						complete(group);
					}
					else {
						partialComplete(group);
					}
				} // If not releasing any messages the group might still be complete
				else if (group.isComplete()) {
					for (Message<?> discard : group.getUnmarked()) {
						discardChannel.send(discard);
					}
					complete(group);
				}
			}
			else {
				discardChannel.send(message);
			}

		}

	}

	// TODO: arrange for this to be called if user desires, e.g. periodically
	public final boolean forceComplete(Object correlationKey) {

		Object lock = getLock(correlationKey);
		synchronized (lock) {

			Collection<Message<?>> all = store.list(correlationKey);
			MessageGroup group = new MessageGroup(all, correlationKey);
			if (all.size() > 0) {
				// last chance for normal completion
				if (ReleaseStrategy.canRelease(group)) {
					outputProcessor.processAndSend(group, channelTemplate, resolveReplyChannel(all.iterator().next(),
							this.outputChannel));
					complete(group);
				}
				else {
					if (sendPartialResultOnTimeout) {
						if (logger.isInfoEnabled()) {
							logger.info("Processing partially complete messages for key [" + correlationKey + "] to: "
									+ outputChannel);
						}
						outputProcessor.processAndSend(group, channelTemplate, resolveReplyChannel(all.iterator()
								.next(), this.outputChannel));
					}
					else {
						if (logger.isInfoEnabled()) {
							logger.info("Discarding partially complete messages for key [" + correlationKey + "] to: "
									+ discardChannel);
						}
						for (Message<?> message : all) {
							discardChannel.send(message);
						}
					}
					complete(group);
				}
				return true;
			}
			return false;
		}
	}

	private Object getLock(Object correlationKey) {
		locks.putIfAbsent(correlationKey, correlationKey);
		return locks.get(correlationKey);
	}

	private void partialComplete(MessageGroup group) {
		for (Message<?> message : group.getUnmarked()) {
			store.mark(group.getCorrelationKey(), message.getHeaders().getId());
		}
	}

	private void complete(MessageGroup group) {
		Object correlationKey = group.getCorrelationKey();
		store.deleteAll(correlationKey);
		locks.remove(correlationKey);
	}

	private void store(Message<?> message, Object correlationKey) {
		store.put(correlationKey, message);
	}

}
