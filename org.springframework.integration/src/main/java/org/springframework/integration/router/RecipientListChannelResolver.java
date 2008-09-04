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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;

/**
 * A {@link ChannelResolver} implementation that routes to a statically
 * configured list of recipients. The recipients are provided either as a list
 * of {@link MessageChannel} instances or as a String array of channel names.
 * For dynamic recipient lists, either implement {@link ChannelResolver} or
 * extend the {@link AbstractMultiChannelNameResolver} base class.
 * 
 * @author Mark Fisher
 */
public class RecipientListChannelResolver extends AbstractChannelResolver implements InitializingBean {

	private volatile List<MessageChannel> channels;

	private volatile String[] channelNames;


	public void setChannels(List<MessageChannel> channels) {
		this.channels = channels;
	}

	public void setChannelNames(String[] channelNames) {
		this.channelNames = channelNames;
	}

	public void afterPropertiesSet() {
		if ((this.channels != null && this.channelNames != null)
				|| (this.channels == null && this.channelNames == null)) {
			throw new ConfigurationException("either 'channels' or 'channelNames' should be provided, but not both");
		}
	}

	@Override
	public Collection<MessageChannel> resolveChannels(Message<?> message) {
		if (this.channels == null && this.channelNames != null) {
			List<MessageChannel> resolved = new ArrayList<MessageChannel>();
			for (String channelName : channelNames) {
				resolved.add(this.lookupChannel(channelName, true));
			}
			return resolved;
		}
		return this.channels;
	}

}
