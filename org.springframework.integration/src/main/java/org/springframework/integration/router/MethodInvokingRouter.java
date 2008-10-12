/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMappingMethodInvoker;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A Message Router that invokes the specified method on the given object. The
 * method's return value may be a single MessageChannel instance, a single
 * String to be interpreted as a channel name, or a Collection (or Array) of
 * either type. If the method returns channel names, then a
 * {@link ChannelResolver} is required.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingRouter extends AbstractMessageRouter implements InitializingBean {

	private final MessageMappingMethodInvoker invoker;

	private volatile ChannelResolver channelResolver;


	public MethodInvokingRouter(Object object, Method method) {
		this.invoker = new MessageMappingMethodInvoker(object, method);
	}

	public MethodInvokingRouter(Object object, String methodName) {
		this.invoker = new MessageMappingMethodInvoker(object, methodName);
	}


	/**
	 * Provide the {@link ChannelResolver} strategy to use for methods that
	 * return a channel name rather than a {@link MessageChannel} instance.
	 */
	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void afterPropertiesSet() throws Exception {
		this.invoker.afterPropertiesSet();
	}

	@Override
	protected final Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		Object result = this.invoker.invokeMethod(message);
		if (result == null) {
			return null;
		}
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		if (result instanceof Collection) {
			for (Object next : (Collection<?>) result) {
				this.addChannel(next, channels);
			}
		}
		else if (result instanceof MessageChannel[]) {
			channels.addAll(Arrays.asList((MessageChannel[]) result));
		}
		else if (result instanceof String[]) {
			for (String channelName : (String[]) result) {
				this.addChannel(channelName, channels);
			}
		}
		else if (result instanceof MessageChannel) {
			channels.add((MessageChannel) result);
		}
		else if (result instanceof String) {
			this.addChannel((String) result, channels);
		}
		else {
			throw new IllegalStateException(
					"router method must return type 'MessageChannel' or 'String'");
		}
		return channels;
	}

	private void addChannel(Object channelOrName, List<MessageChannel> channels) {
		if (channelOrName == null) {
			return;
		}
		if (channelOrName instanceof MessageChannel) {
			channels.add((MessageChannel) channelOrName);
		}
		else if (channelOrName instanceof String) {
			String channelName = (String) channelOrName;
			Assert.state(this.channelResolver != null,
					"unable to resolve channel names, no ChannelResolver available");
			MessageChannel channel = this.channelResolver.resolveChannelName(channelName);
			if (channel == null) {
				throw new MessagingException("failed to resolve channel name '" + channelName + "'");
			}
			channels.add(channel);
		}
		else {
			throw new MessagingException("unsupported return type for router [" + channelOrName.getClass() + "]");
		}
	}

}
