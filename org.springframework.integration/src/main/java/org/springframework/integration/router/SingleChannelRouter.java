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

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;

/**
 * A router implementation for sending to at most one {@link MessageChannel}.
 * Requires either a {@link ChannelResolver} or {@link ChannelNameResolver}
 * strategy instance.
 * 
 * @author Mark Fisher
 */
public class SingleChannelRouter extends AbstractRouter implements InitializingBean {

	private ChannelResolver channelResolver;

	private ChannelNameResolver channelNameResolver;


	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setChannelNameResolver(ChannelNameResolver channelNameResolver) {
		this.channelNameResolver = channelNameResolver;
	}

	public void afterPropertiesSet() {
		if (!(this.channelResolver != null ^ this.channelNameResolver != null)) {
			throw new ConfigurationException(
					"exactly one of 'channelResolver' or 'channelNameResolver' must be provided");
		}
	}

	@Override
	protected Collection<?> resolveChannels(Message<?> message) {
		Object result = (this.channelResolver != null)
				? this.channelResolver.resolve(message)
				: this.channelNameResolver.resolve(message);
		if (result == null) {
			return null;
		}
		return Collections.singletonList(result);
	}

}
