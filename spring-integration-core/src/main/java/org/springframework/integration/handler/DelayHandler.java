/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.handler;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.ChannelResolutionException;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} that is capable of delaying the continuation of a
 * Message flow based on the presence of a delay header on an inbound Message
 * or a default delay value configured on this handler. Note that the
 * continuation of the flow is delegated to a {@link TaskScheduler}, and
 * therefore, the calling thread does not block. The advantage of this approach
 * is that many delays can be managed concurrently, even very long delays,
 * without producing a buildup of blocked Threads.
 * <p>
 * One thing to keep in mind, however, is that any active transactional context
 * will not propagate from the original sender to the eventual recipient. This
 * is a side-effect of passing the Message to the output channel after the
 * delay with a different Thread in control.
 * <p>
 * When this handler's 'delayHeaderName' property is configured, that value, if
 * present on a Message, will take precedence over the handler's 'defaultDelay'
 * value. The actual header value may be a long, a String that can be parsed
 * as a long, or a Date. If it is a long, it will be interpreted as the length
 * of time to delay in milliseconds counting from the current time (e.g. a
 * value of 5000 indicates that the Message can be released as soon as five
 * seconds from the current time). If the value is a Date, it will be
 * delayed at least until that Date occurs (i.e. the delay in that case is
 * equivalent to <code>headerDate.getTime() - new Date().getTime()</code>).
 * 
 * @author Mark Fisher
 * @since 1.0.3
 */
public class DelayHandler extends IntegrationObjectSupport implements MessageHandler, MessageProducer, Ordered, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile long defaultDelay;

	private volatile String delayHeaderName;

	private boolean waitForTasksToCompleteOnShutdown = false;

	private volatile MessageChannel outputChannel;

	private volatile ChannelResolver channelResolver;

	private volatile MessageStore messageStore;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * Create a DelayHandler with the given default delay. The sending of Messages after
	 * the delay will be handled by a scheduled thread pool with a size of 1.
	 */
	public DelayHandler(long defaultDelay) {
		this(defaultDelay, null);
	}

	/**
	 * Create a DelayHandler with the given default delay. The sending of Messages
	 * after the delay will be handled by the provided {@link TaskScheduler}.
	 */
	public DelayHandler(long defaultDelay, TaskScheduler taskScheduler) {
		this.defaultDelay = defaultDelay;
		this.setTaskScheduler(taskScheduler != null ? taskScheduler : new ThreadPoolTaskScheduler());
	}


	/**
	 * Set the default delay in milliseconds. If no 'delayHeaderName' property
	 * has been provided, the default delay will be applied to all Messages. If
	 * a delay should <emphasis>only</emphasis> be applied to Messages with a
	 * header, then set this value to 0. 
	 */
	public void setDefaultDelay(long defaultDelay) {
		this.defaultDelay = defaultDelay;
	}

	/**
	 * Specify the name of the header that should be checked for a delay period
	 * (in milliseconds) or a Date to delay until. If this property is set, any
	 * such header value will take precedence over this handler's default delay.
	 */
	public void setDelayHeaderName(String delayHeaderName) {
		this.delayHeaderName = delayHeaderName;
	}

	/**
	 * Specify the {@link MessageStore} that should be used to store Messages
	 * while awaiting the delay.
	 */
	public void setMessageStore(MessageStore messageStore) {
		this.messageStore = messageStore;
	}

	/**
	 * Set the output channel for this handler. If none is provided, each
	 * inbound Message must include a reply channel header.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Set the timeout for sending reply Messages.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Set whether to wait for scheduled tasks to complete on shutdown.
	 * <p>Default is "false". Switch this to "true" if you prefer
	 * fully completed tasks at the expense of a longer shutdown phase.
	 * <p>
	 * This property will only have an effect for TaskScheduler implementations
	 * that extend from {@link ExecutorConfigurationSupport}.
	 * @see ExecutorConfigurationSupport#setWaitForTasksToCompleteOnShutdown(boolean)
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	@Override
	public String getComponentType() {
		return "delayer";
	}

	protected void onInit() throws Exception{
		if (this.getTaskScheduler() instanceof ExecutorConfigurationSupport) {
			((ExecutorConfigurationSupport) this.getTaskScheduler()).setWaitForTasksToCompleteOnShutdown(this.waitForTasksToCompleteOnShutdown);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("The 'waitForJobsToCompleteOnShutdown' property is not supported for TaskScheduler of type [" +
					this.getTaskScheduler().getClass() + "]");
		}
		if (this.messageStore == null) {
			this.messageStore = new SimpleMessageStore();
		}
		if (this.getTaskScheduler() instanceof InitializingBean) {
			((InitializingBean) this.getTaskScheduler()).afterPropertiesSet();
		}
		if (this.getBeanFactory() != null){
			this.channelResolver = new BeanFactoryChannelResolver(this.getBeanFactory());
		}
	}

	public final void handleMessage(final Message<?> message) {
		long delay = this.determineDelayForMessage(message);
		if (delay > 0) {
			this.releaseMessageAfterDelay(message, delay);
		}
		else {
			// no delay, send directly
			this.sendMessageToReplyChannel(message);
		}
	}

	private long determineDelayForMessage(Message<?> message) {
		long delay = this.defaultDelay;
		if (this.delayHeaderName != null) {
			Object headerValue = message.getHeaders().get(this.delayHeaderName);
			if (headerValue instanceof Date) {
				delay = ((Date) headerValue).getTime() - new Date().getTime();
			}
			else if (headerValue != null) {
				try {
					delay = Long.valueOf(headerValue.toString());
				}
				catch (NumberFormatException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to parse delay from header value '" + headerValue.toString() +
								"', will fall back to default delay: " + this.defaultDelay);
					}
				}
			}
		}
		return delay;
	}

	private void releaseMessageAfterDelay(final Message<?> message, long delay) {
		Assert.state(this.messageStore != null, "MessageStore must not be null");
		final Message<?> storedMessage = this.messageStore.addMessage(message);
		this.getTaskScheduler().schedule(new Runnable() {
			public void run() {
				try {
					releaseMessage(storedMessage.getHeaders().getId());
				}
				catch (Exception e) {
					Exception exception = new MessageHandlingException(message, "Failed to deliver Message after delay.", e);
					MessageChannel errorChannel = resolveErrorChannelIfPossible(message);
					if (errorChannel != null) {
						ErrorMessage errorMessage = new ErrorMessage(exception);
						try {
							messagingTemplate.send(errorChannel, errorMessage);
						}
						catch (Exception e2) {
							if (logger.isWarnEnabled()) {
								logger.warn("Failed to send MessagingException to error channel.", exception);
							}
						}
					}
					else if (logger.isWarnEnabled()) {
						logger.warn("No error channel available. MessagingException will be ignored.", exception);
					}
				}
			}
		}, new Date(System.currentTimeMillis() + delay));
	}

	private void releaseMessage(UUID id) {
		Assert.state(this.messageStore != null, "MessageStore must not be null");
		Message<?> message = this.messageStore.removeMessage(id);
		Assert.notNull(message, "Message with id: " + id + " no longer exists in MessageStore.");
		this.sendMessageToReplyChannel(message);
	}

	private void sendMessageToReplyChannel(Message<?> message) {
		MessageChannel replyChannel = this.resolveReplyChannel(message);
		this.messagingTemplate.send(replyChannel, message);
	}

	private MessageChannel resolveReplyChannel(Message<?> message) {
		MessageChannel replyChannel = this.outputChannel;
		if (replyChannel == null) {
			replyChannel = this.resolveChannelFromHeader(message, MessageHeaders.REPLY_CHANNEL);
		}
		if (replyChannel == null) {
			throw new ChannelResolutionException(
					"unable to resolve reply channel for message: " + message);
		}
		return replyChannel;
	}

	private MessageChannel resolveErrorChannelIfPossible(Message<?> message) {
		MessageChannel errorChannel = null;
		try {
			errorChannel = this.resolveChannelFromHeader(message, MessageHeaders.ERROR_CHANNEL);
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to resolve error channel from header.", e);
			}
		}
		if (errorChannel == null && this.channelResolver != null) {
			errorChannel = this.channelResolver.resolveChannelName(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		}
		return errorChannel;
	}

	private MessageChannel resolveChannelFromHeader(Message<?> message, String headerName) {
		MessageChannel channel = null;
		Object channelHeader = message.getHeaders().get(headerName);
		if (channelHeader != null) {
			if (channelHeader instanceof MessageChannel) {
				channel = (MessageChannel) channelHeader;
			}
			else if (channelHeader instanceof String) {
				Assert.state(this.channelResolver != null,
						"ChannelResolver is required for resolving '" + headerName + "' by name.");
				channel = this.channelResolver.resolveChannelName((String) channelHeader);
			}
			else {
				throw new ChannelResolutionException("expected a MessageChannel or String for '" +
						headerName + "', but type is [" + channelHeader.getClass() + "]");
			}
		}
		return channel;
	}

	public void destroy() throws Exception {
		if (this.getTaskScheduler() instanceof DisposableBean) {
			((DisposableBean) this.getTaskScheduler()).destroy();
		}
	}
}
