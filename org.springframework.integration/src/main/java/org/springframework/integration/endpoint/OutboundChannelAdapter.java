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

package org.springframework.integration.endpoint;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.util.Assert;

/**
 * A Channel Adapter implementation for connecting a {@link MessageChannel}
 * to a {@link org.springframework.integration.message.MessageTarget}.
 * 
 * @author Mark Fisher
 */
public class OutboundChannelAdapter extends AbstractEndpoint {

	private final MessageTarget target;


	public OutboundChannelAdapter(MessageTarget target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
	}


	@Override
	protected boolean sendInternal(Message<?> message) {
		return this.target.send(message);
	}

}
