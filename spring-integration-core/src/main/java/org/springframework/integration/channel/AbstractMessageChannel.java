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

package org.springframework.integration.channel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.core.OrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.support.converter.DefaultDatatypeChannelMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;
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
public abstract class AbstractMessageChannel extends IntegrationObjectSupport
		implements MessageChannel, TrackableComponent, ChannelInterceptorAware {

	private volatile boolean shouldTrack = false;

	private volatile Class<?>[] datatypes = new Class<?>[0];

	private final ChannelInterceptorList interceptors = new ChannelInterceptorList();

	private volatile String fullChannelName;

	private volatile MessageConverter messageConverter;

	@Override
	public String getComponentType() {
		return "channel";
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	/**
	 * Specify the Message payload datatype(s) supported by this channel. If a
	 * payload type does not match directly, but the 'conversionService' is
	 * available, then type conversion will be attempted in the order of the
	 * elements provided in this array.
	 * <p>
	 * If this property is not set explicitly, any Message payload type will be
	 * accepted.
	 *
	 * @param datatypes The supported data types.
	 *
	 * @see #setMessageConverter(MessageConverter)
	 */
	public void setDatatypes(Class<?>... datatypes) {
		this.datatypes = (datatypes != null && datatypes.length > 0)
				? datatypes : new Class<?>[0];
	}

	/**
	 * Set the list of channel interceptors. This will clear any existing
	 * interceptors.
	 *
	 * @param interceptors The list of interceptors.
	 */
	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		Collections.sort(interceptors, new OrderComparator());
		this.interceptors.set(interceptors);
	}

	/**
	 * Add a channel interceptor to the end of the list.
	 *
	 * @param interceptor The interceptor.
	 */
	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		this.interceptors.add(interceptor);
	}

	/**
	 * Add a channel interceptor to the specified index of the list.
	 *
	 * @param index The index to add interceptor.
	 * @param interceptor The interceptor.
	 */
	@Override
	public void addInterceptor(int index, ChannelInterceptor interceptor) {
		this.interceptors.add(index, interceptor);
	}

	/**
	 * Specify the {@link ConversionService} to use when trying to convert to
	 * one of this channel's supported datatypes for a Message whose payload
	 * does not already match. If this property is not set explicitly but
	 * the channel is managed within a context, it will attempt to locate a
	 * bean named "integrationConversionService" defined within that context.
	 *
	 * @param conversionService The conversion service.
	 * @deprecated No longer used; see {@link DefaultDatatypeChannelMessageConverter}.
	 */
	@Deprecated
	@Override
	public void setConversionService(ConversionService conversionService) {
		if (logger.isWarnEnabled()) {
			logger.warn("The conversion service is no longer used; see setMessageConverter()");
		}
	}

	/**
	 * Specify the {@link MessageConverter} to use when trying to convert to
	 * one of this channel's supported datatypes (in order) for a Message whose payload
	 * does not already match.
	 * <p>
	 * <b>Note:</b> only the {@link MessageConverter#fromMessage(Message, Class)}
	 * method is used. If the returned object is not a {@link Message}, the inbound
	 * headers will be copied; if the returned object is a {@code Message}, it is
	 * expected that the converter will have fully populated the headers; no
	 * further action is performed by the channel. If {@code null} is returned,
	 * conversion to the next datatype (if any) will be attempted.
	 *
	 * Defaults to a {@link DefaultDatatypeChannelMessageConverter}.
	 *
	 * @param messageConverter The message converter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return a read-only list of the configured interceptors.
	 */
	@Override
	public List<ChannelInterceptor> getChannelInterceptors() {
		return this.interceptors.getInterceptors();
	}

	/**
	 * Exposes the interceptor list for subclasses.
	 *
	 * @return The channel interceptor list.
	 */
	protected ChannelInterceptorList getInterceptors() {
		return this.interceptors;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.messageConverter == null) {
			if (this.getBeanFactory() != null) {
				if (this.getBeanFactory().containsBean(
						IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME)) {
					this.messageConverter = this.getBeanFactory().getBean(
							IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME,
							MessageConverter.class);
				}
			}
		}
	}

	/**
	 * Returns the fully qualified channel name including the application context
	 * id, if available.
	 *
	 * @return The name.
	 */
	public String getFullChannelName() {
		if (this.fullChannelName == null) {
			String contextId = this.getApplicationContextId();
			String componentName = this.getComponentName();
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
	 *
	 * @param message the Message to send
	 *
	 * @return <code>true</code> if the message is sent successfully or
	 * <code>false</code> if the sending thread is interrupted.
	 */
	@Override
	public final boolean send(Message<?> message) {
		return this.send(message, -1);
	}

	/**
	 * Send a message on this channel. If the channel is at capacity, this
	 * method will block until either the timeout occurs or the sending thread
	 * is interrupted. If the specified timeout is 0, the method will return
	 * immediately. If less than zero, it will block indefinitely (see
	 * {@link #send(Message)}).
	 *
	 * @param message the Message to send
	 * @param timeout the timeout in milliseconds
	 *
	 * @return <code>true</code> if the message is sent successfully,
	 * <code>false</code> if the message cannot be sent within the allotted
	 * time or the sending thread is interrupted.
	 */
	@Override
	public final boolean send(Message<?> message, long timeout) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(message.getPayload(), "message payload must not be null");
		if (this.shouldTrack) {
			message = MessageHistory.write(message, this, this.getMessageBuilderFactory());
		}
		try {
			if (this.datatypes.length > 0) {
				message = this.convertPayloadIfNecessary(message);
			}
			message = this.interceptors.preSend(message, this);
			if (message == null) {
				return false;
			}
			boolean sent = this.doSend(message, timeout);
			this.interceptors.postSend(message, this, sent);
			return sent;
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			throw new MessageDeliveryException(message,
					"failed to send Message to channel '" + this.getComponentName() + "'", e);
		}
	}

	private Message<?> convertPayloadIfNecessary(Message<?> message) {
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
						return this.getMessageBuilderFactory().withPayload(converted).copyHeaders(message.getHeaders()).build();
					}
				}
			}
		}
		throw new MessageDeliveryException(message, "Channel '" + this.getComponentName() +
				"' expected one of the following datataypes [" +
				StringUtils.arrayToCommaDelimitedString(this.datatypes) +
				"], but received [" + message.getPayload().getClass() + "]");
	}

	/**
	 * Subclasses must implement this method. A non-negative timeout indicates
	 * how long to wait if the channel is at capacity (if the value is 0, it
	 * must return immediately with or without success). A negative timeout
	 * value indicates that the method should block until either the message is
	 * accepted or the blocking thread is interrupted.
	 *
	 * @param message The message.
	 * @param timeout The timeout.
	 * @return true if the send was successful.
	 */
	protected abstract boolean doSend(Message<?> message, long timeout);


	/**
	 * A convenience wrapper class for the list of ChannelInterceptors.
	 */
	protected class ChannelInterceptorList {

		private final List<ChannelInterceptor> interceptors = new CopyOnWriteArrayList<ChannelInterceptor>();


		public boolean set(List<ChannelInterceptor> interceptors) {
			synchronized (this.interceptors) {
				this.interceptors.clear();
				return this.interceptors.addAll(interceptors);
			}
		}

		public boolean add(ChannelInterceptor interceptor) {
			return this.interceptors.add(interceptor);
		}

		public void add(int index, ChannelInterceptor interceptor) {
			this.interceptors.add(index, interceptor);
		}

		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			if (logger.isDebugEnabled()) {
				logger.debug("preSend on channel '" + channel + "', message: " + message);
			}
			if (this.interceptors.size() > 0) {
				for (ChannelInterceptor interceptor : this.interceptors) {
					message = interceptor.preSend(message, channel);
					if (message == null) {
						return null;
					}
				}
			}
			return message;
		}

		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
			if (logger.isDebugEnabled()) {
				logger.debug("postSend (sent=" + sent + ") on channel '" + channel + "', message: " + message);
			}
			if (this.interceptors.size() > 0) {
				for (ChannelInterceptor interceptor : interceptors) {
					interceptor.postSend(message, channel, sent);
				}
			}
		}

		public boolean preReceive(MessageChannel channel) {
			if (logger.isTraceEnabled()) {
				logger.trace("preReceive on channel '" + channel + "'");
			}
			if (this.interceptors.size() > 0) {
				for (ChannelInterceptor interceptor : interceptors) {
					if (!interceptor.preReceive(channel)) {
						return false;
					}
				}
			}
			return true;
		}

		public Message<?> postReceive(Message<?> message, MessageChannel channel) {
			if (message != null && logger.isDebugEnabled()) {
				logger.debug("postReceive on channel '" + channel + "', message: " + message);
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("postReceive on channel '" + channel + "', message is null");
			}
			if (this.interceptors.size() > 0) {
				for (ChannelInterceptor interceptor : interceptors) {
					message = interceptor.postReceive(message, channel);
					if (message == null) {
						return null;
					}
				}
			}
			return message;
		}

		public List<ChannelInterceptor> getInterceptors() {
			return Collections.unmodifiableList(this.interceptors);
		}
	}
}
