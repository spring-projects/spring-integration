/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.List;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;

/**
 * A simple extension of {@link MultiChannelRouter} that routes to a statically
 * configured list of recipients. The recipients are provided either as a list
 * of {@link MessageChannel} instances or as a String array of channel names.
 * For dynamic recipient lists, implement either {@link MultiChannelResolver} or
 * {@link MultiChannelNameResolver} and then explicitly configure an instance of
 * {@link MultiChannelRouter}.
 * 
 * @author Mark Fisher
 */
public class RecipientListRouter extends MultiChannelRouter {

	public void setChannelNames(String[] channelNames) {
		this.setChannelNameResolver(new RecipientListChannelNameResolver(channelNames));
	}

	public void setChannels(List<MessageChannel> channels) {
		this.setChannelResolver(new RecipientListChannelResolver(channels));
	}


	private static class RecipientListChannelResolver implements MultiChannelResolver {

		private List<MessageChannel> channels;

		RecipientListChannelResolver(List<MessageChannel> channels) {
			this.channels = channels;
		}

		public List<MessageChannel> resolve(Message<?> message) {
			return this.channels;
		}
	}


	private static class RecipientListChannelNameResolver implements MultiChannelNameResolver {

		private String[] channelNames;

		RecipientListChannelNameResolver(String[] channelNames) {
			this.channelNames = channelNames;
		}

		public String[] resolve(Message<?> message) {
			return this.channelNames;
		}
	}

}
