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

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.util.ObjectUtils;

/**
 * A router implementation for sending to potentially multiple {@link MessageChannel MessageChannels}.
 * Requires either a {@link MultiChannelResolver} or {@link MultiChannelNameResolver} strategy instance.
 * 
 * @author Mark Fisher
 */
public class MultiChannelRouter extends AbstractRouter implements InitializingBean {

	private volatile MultiChannelResolver channelResolver;

	private volatile MultiChannelNameResolver channelNameResolver;


	public void setChannelResolver(MultiChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setChannelNameResolver(MultiChannelNameResolver channelNameResolver) {
		this.channelNameResolver = channelNameResolver;
	}

	public void afterPropertiesSet() {
		if (!(this.channelResolver != null ^ this.channelNameResolver != null)) {
			throw new ConfigurationException(
					"exactly one of 'channelResolver' or 'channelNameResolver' must be provided");
		}
	}

	@Override
	public Collection<?> resolveChannels(Message<?> message) {
		if (this.channelResolver != null) {
			return this.channelResolver.resolve(message);
		}
		String[] channelNames = this.channelNameResolver.resolve(message);
		if (ObjectUtils.isEmpty(channelNames)) {
			return null;
		}
		return Arrays.asList(channelNames);
	}

}
