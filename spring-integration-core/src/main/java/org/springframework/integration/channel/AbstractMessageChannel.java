/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.OrderComparator;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.integration.support.management.metrics.MeterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.management.metrics.SampleFacade;
import org.springframework.integration.support.management.metrics.TimerFacade;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link MessageChannel} implementations providing common
 * properties such as the channel name. Also provides the common functionality
 * for sending and receiving {@link Message Messages} including the invocation
 * of any {@link org.springframework.messaging.support.ChannelInterceptor ChannelInterceptors}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@IntegrationManagedResource
public abstract class AbstractMessageChannel extends IntegrationObjectSupport
		implements MessageChannel, TrackableComponent, InterceptableChannel, IntegrationManagement, IntegrationPattern {

	protected final ChannelInterceptorList interceptors = new ChannelInterceptorList(this.logger); // NOSONAR

	private final Comparator<Object> orderComparator = new OrderComparator();

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	protected final Set<MeterFacade> meters = ConcurrentHashMap.newKeySet(); // NOSONAR

	private volatile boolean shouldTrack = false;

	private volatile Class<?>[] datatypes = new Class<?>[0];

	private volatile String fullChannelName;

	private volatile MessageConverter messageConverter;

	private volatile boolean loggingEnabled = true;

	private MetricsCaptor metricsCaptor;

	private TimerFacade successTimer;

	private TimerFacade failureTimer;

	@Override
	public String getComponentType() {
		return "channel";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.message_channel;
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	public void registerMetricsCaptor(MetricsCaptor metricsCaptorToRegister) {
		this.metricsCaptor = metricsCaptorToRegister;
	}

	@Nullable
	protected MetricsCaptor getMetricsCaptor() {
		return this.metricsCaptor;
	}

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	@Override
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
		this.managementOverrides.loggingConfigured = true;
	}

	/**
	 * Specify the Message payload datatype(s) supported by this channel. If a
	 * payload type does not match directly, but the 'conversionService' is
	 * available, then type conversion will be attempted in the order of the
	 * elements provided in this array.
	 * <p> If this property is not set explicitly, any Message payload type will be
	 * accepted.
	 * @param datatypes The supported data types.
	 * @see #setMessageConverter(MessageConverter)
	 */
	public void setDatatypes(Class<?>... datatypes) {
		this.datatypes =
				(datatypes != null && datatypes.length > 0)
						? datatypes
						: new Class<?>[0];
	}

	/**
	 * Set the list of channel interceptors. This will clear any existing
	 * interceptors.
	 * @param interceptors The list of interceptors.
	 */
	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		interceptors.sort(this.orderComparator);
		this.interceptors.set(interceptors);
	}

	/**
	 * Add a channel interceptor to the end of the list.
	 * @param interceptor The interceptor.
	 */
	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		this.interceptors.add(interceptor);
	}

	/**
	 * Add a channel interceptor to the specified index of the list.
	 * @param index The index to add interceptor.
	 * @param interceptor The interceptor.
	 */
	@Override
	public void addInterceptor(int index, ChannelInterceptor interceptor) {
		this.interceptors.add(index, interceptor);
	}

	/**
	 * Specify the {@link MessageConverter} to use when trying to convert to
	 * one of this channel's supported datatypes (in order) for a Message whose payload
	 * does not already match.
	 * <p> <b>Note:</b> only the {@link MessageConverter#fromMessage(Message, Class)}
	 * method is used. If the returned object is not a {@link Message}, the inbound
	 * headers will be copied; if the returned object is a {@code Message}, it is
	 * expected that the converter will have fully populated the headers; no
	 * further action is performed by the channel. If {@code null} is returned,
	 * conversion to the next datatype (if any) will be attempted.
	 * Defaults to a
	 * {@link org.springframework.integration.support.converter.DefaultDatatypeChannelMessageConverter}.
	 * @param messageConverter The message converter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return a read-only list of the configured interceptors.
	 */
	@Override
	public List<ChannelInterceptor> getInterceptors() {
		return this.interceptors.getInterceptors();
	}

	@Override
	public boolean removeInterceptor(ChannelInterceptor interceptor) {
		return this.interceptors.remove(interceptor);
	}

	@Override
	@Nullable
	public ChannelInterceptor removeInterceptor(int index) {
		return this.interceptors.remove(index);
	}

	/**
	 * Exposes the interceptor list instance for subclasses.
	 * @return The channel interceptor list.
	 */
	protected ChannelInterceptorList getIChannelInterceptorList() {
		return this.interceptors;
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.managementOverrides;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.messageConverter == null) {
			BeanFactory beanFactory = getBeanFactory();
			if (beanFactory != null &&
					beanFactory.containsBean(
							IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME)) {

				this.messageConverter =
						beanFactory.getBean(
								IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME,
								MessageConverter.class);
			}
		}
		this.fullChannelName = null;
	}

	/**
	 * Returns the fully qualified channel name including the application context
	 * id, if available.
	 *
	 * @return The name.
	 */
	public String getFullChannelName() {
		if (this.fullChannelName == null) {
			String contextId = getApplicationContextId();
			String componentName = getComponentName();
			componentName = (StringUtils.hasText(contextId) ? contextId + "." : "") +
					(StringUtils.hasText(componentName) ? componentName : "unknown.channel.name");
			this.fullChannelName = componentName;
		}
		return this.fullChannelName;
	}

	/**
	 * Send a message on this channel. If the channel is at capacity, this
	 * method will block until either space becomes available or the sending
	 * thread is interrupted.
	 * @param message the Message to send
	 * @return <code>true</code> if the message is sent successfully or
	 * <code>false</code> if the sending thread is interrupted.
	 */
	@Override
	public boolean send(Message<?> message) {
		return send(message, -1);
	}

	/**
	 * Send a message on this channel. If the channel is at capacity, this
	 * method will block until either the timeout occurs or the sending thread
	 * is interrupted. If the specified timeout is 0, the method will return
	 * immediately. If less than zero, it will block indefinitely (see
	 * {@link #send(Message)}).
	 * @param messageArg the Message to send
	 * @param timeout the timeout in milliseconds
	 * @return <code>true</code> if the message is sent successfully,
	 * <code>false</code> if the message cannot be sent within the allotted
	 * time or the sending thread is interrupted.
	 */
	@Override // NOSONAR complexity
	public boolean send(Message<?> messageArg, long timeout) {
		Assert.notNull(messageArg, "message must not be null");
		Assert.notNull(messageArg.getPayload(), "message payload must not be null");
		Message<?> message = messageArg;
		if (this.shouldTrack) {
			message = MessageHistory.write(message, this, getMessageBuilderFactory());
		}

		Deque<ChannelInterceptor> interceptorStack = null;
		boolean sent = false;
		boolean metricsProcessed = false;
		ChannelInterceptorList interceptorList = this.interceptors;
		SampleFacade sample = null;
		try {
			message = convertPayloadIfNecessary(message);
			boolean debugEnabled = this.loggingEnabled && this.logger.isDebugEnabled();
			if (debugEnabled) {
				logger.debug("preSend on channel '" + this + "', message: " + message);
			}
			if (interceptorList.getSize() > 0) {
				interceptorStack = new ArrayDeque<>();
				message = interceptorList.preSend(message, this, interceptorStack);
				if (message == null) {
					return false;
				}
			}
			if (this.metricsCaptor != null) {
				sample = this.metricsCaptor.start();
			}
			sent = doSend(message, timeout);
			if (sample != null) {
				sample.stop(sendTimer(sent));
			}
			metricsProcessed = true;

			if (debugEnabled) {
				logger.debug("postSend (sent=" + sent + ") on channel '" + this + "', message: " + message);
			}
			if (interceptorStack != null) {
				interceptorList.postSend(message, this, sent);
				interceptorList.afterSendCompletion(message, this, sent, null, interceptorStack);
			}
			return sent;
		}
		catch (Exception ex) {
			if (!metricsProcessed && sample != null) {
				sample.stop(buildSendTimer(false, ex.getClass().getSimpleName()));
			}
			if (interceptorStack != null) {
				interceptorList.afterSendCompletion(message, this, sent, ex, interceptorStack);
			}
			throw IntegrationUtils.wrapInDeliveryExceptionIfNecessary(message,
					() -> "failed to send Message to channel '" + getComponentName() + "'", ex);
		}
	}

	private TimerFacade sendTimer(boolean sent) {
		if (sent) {
			if (this.successTimer == null) {
				this.successTimer = buildSendTimer(true, "none");
			}
			return this.successTimer;
		}
		else {
			if (this.failureTimer == null) {
				this.failureTimer = buildSendTimer(false, "none");
			}
			return this.failureTimer;
		}
	}

	private TimerFacade buildSendTimer(boolean success, String exception) {
		TimerFacade timer = this.metricsCaptor.timerBuilder(SEND_TIMER_NAME)
				.tag("type", "channel")
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("result", success ? "success" : "failure")
				.tag("exception", exception)
				.description("Send processing time")
				.build();
		this.meters.add(timer);
		return timer;
	}

	private Message<?> convertPayloadIfNecessary(Message<?> message) {
		if (this.datatypes.length > 0) {
			// first pass checks if the payload type already matches any of the datatypes
			for (Class<?> datatype : this.datatypes) {
				if (datatype.isAssignableFrom(message.getPayload().getClass())) {
					return message;
				}
			}
			if (this.messageConverter != null) {
				// second pass applies conversion if possible, attempting datatypes in order
				for (Class<?> datatype : this.datatypes) {
					Object converted = this.messageConverter.fromMessage(message, datatype);
					if (converted != null) {
						if (converted instanceof Message) {
							return (Message<?>) converted;
						}
						else {
							return getMessageBuilderFactory()
									.withPayload(converted)
									.copyHeaders(message.getHeaders())
									.build();
						}
					}
				}
			}
			throw new MessageDeliveryException(message, "Channel '" + getComponentName() +
					"' expected one of the following data types [" +
					StringUtils.arrayToCommaDelimitedString(this.datatypes) +
					"], but received [" + message.getPayload().getClass() + "]");
		}
		else {
			return message;
		}
	}

	/**
	 * Subclasses must implement this method. A non-negative timeout indicates
	 * how long to wait if the channel is at capacity (if the value is 0, it
	 * must return immediately with or without success). A negative timeout
	 * value indicates that the method should block until either the message is
	 * accepted or the blocking thread is interrupted.
	 * @param message The message.
	 * @param timeout The timeout.
	 * @return true if the send was successful.
	 */
	protected abstract boolean doSend(Message<?> message, long timeout);

	@Override
	public void destroy() {
		this.meters.forEach(MeterFacade::remove);
		this.meters.clear();
	}

	/**
	 * A convenience wrapper class for the list of ChannelInterceptors.
	 */
	protected static class ChannelInterceptorList {

		protected final List<ChannelInterceptor> interceptors = new CopyOnWriteArrayList<>(); // NOSONAR

		private final LogAccessor logger;

		private int size;

		public ChannelInterceptorList(LogAccessor logger) {
			this.logger = logger;
		}

		public boolean set(List<ChannelInterceptor> interceptors) {
			synchronized (this.interceptors) {
				this.interceptors.clear();
				this.size = interceptors.size();
				return this.interceptors.addAll(interceptors);
			}
		}

		public int getSize() {
			return this.size;
		}

		public boolean add(ChannelInterceptor interceptor) {
			this.size++;
			return this.interceptors.add(interceptor);
		}

		public void add(int index, ChannelInterceptor interceptor) {
			this.size++;
			this.interceptors.add(index, interceptor);
		}

		@Nullable
		public Message<?> preSend(Message<?> messageArg, MessageChannel channel,
				Deque<ChannelInterceptor> interceptorStack) {

			Message<?> message = messageArg;
			if (this.size > 0) {
				for (ChannelInterceptor interceptor : this.interceptors) {
					Message<?> previous = message;
					message = interceptor.preSend(message, channel);
					if (message == null) {
						this.logger.debug(() -> interceptor.getClass().getSimpleName()
								+ " returned null from preSend, i.e. precluding the send.");
						afterSendCompletion(previous, channel, false, null, interceptorStack);
						return null;
					}
					interceptorStack.add(interceptor);
				}
			}
			return message;
		}

		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
			if (this.size > 0) {
				for (ChannelInterceptor interceptor : this.interceptors) {
					interceptor.postSend(message, channel, sent);
				}
			}
		}

		public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent,
				@Nullable Exception ex, Deque<ChannelInterceptor> interceptorStack) {

			for (Iterator<ChannelInterceptor> iterator = interceptorStack.descendingIterator(); iterator.hasNext(); ) {
				ChannelInterceptor interceptor = iterator.next();
				try {
					interceptor.afterSendCompletion(message, channel, sent, ex);
				}
				catch (Exception ex2) {
					this.logger.error(ex2, () -> "Exception from afterSendCompletion in " + interceptor);
				}
			}
		}

		public boolean preReceive(MessageChannel channel, Deque<ChannelInterceptor> interceptorStack) {
			if (this.size > 0) {
				for (ChannelInterceptor interceptor : this.interceptors) {
					if (!interceptor.preReceive(channel)) {
						afterReceiveCompletion(null, channel, null, interceptorStack);
						return false;
					}
					interceptorStack.add(interceptor);
				}
			}
			return true;
		}

		@Nullable
		public Message<?> postReceive(Message<?> messageArg, MessageChannel channel) {
			Message<?> message = messageArg;
			if (this.size > 0) {
				for (ChannelInterceptor interceptor : this.interceptors) {
					message = interceptor.postReceive(message, channel);
					if (message == null) {
						return null;
					}
				}
			}
			return message;
		}

		public void afterReceiveCompletion(@Nullable Message<?> message, MessageChannel channel,
				@Nullable Exception ex, @Nullable Deque<ChannelInterceptor> interceptorStack) {

			if (interceptorStack != null) {
				for (Iterator<ChannelInterceptor> iter = interceptorStack.descendingIterator(); iter.hasNext(); ) {
					ChannelInterceptor interceptor = iter.next();
					try {
						interceptor.afterReceiveCompletion(message, channel, ex);
					}
					catch (Exception ex2) {
						this.logger.error(ex2, () -> "Exception from afterReceiveCompletion in " + interceptor);
					}
				}
			}
		}

		public List<ChannelInterceptor> getInterceptors() {
			return Collections.unmodifiableList(this.interceptors);
		}

		public boolean remove(ChannelInterceptor interceptor) {
			if (this.interceptors.remove(interceptor)) {
				this.size--;
				return true;
			}
			else {
				return false;
			}
		}

		@Nullable
		public ChannelInterceptor remove(int index) {
			ChannelInterceptor removed = this.interceptors.remove(index);
			if (removed != null) {
				this.size--;
			}
			return removed;
		}

	}

}
