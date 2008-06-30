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

import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.SimpleDispatcher;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.PollCommand;
import org.springframework.integration.message.Source;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves messages from a {@link Source}
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class SourceEndpoint extends AbstractEndpoint {

	private final Source<?> source;

	private final SimpleDispatcher dispatcher = new SimpleDispatcher(new DispatcherPolicy());

	private volatile Schedule schedule;


	public SourceEndpoint(Source<?> source, MessageChannel channel) {
		Assert.notNull(source, "source must not be null");
		Assert.notNull(channel, "channel must not be null");
		this.source = source;
		this.dispatcher.subscribe(channel);
	}


	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	protected boolean supports(Message<?> message) {
		return (message.getPayload() instanceof PollCommand);
	}

	public final boolean doInvoke(Message<?> pollCommandMessage) {
		Message<?> message = this.source.receive();
		if (message == null) {
			return false;
		}
		boolean sent = this.dispatcher.dispatch(message);
		if (this.source instanceof MessageDeliveryAware) {
			if (sent) {
				((MessageDeliveryAware) this.source).onSend(message);
			}
			else {
				((MessageDeliveryAware) this.source).onFailure(new MessageDeliveryException(message, "failed to send message"));
			}
		}
		return sent;
	}

}
