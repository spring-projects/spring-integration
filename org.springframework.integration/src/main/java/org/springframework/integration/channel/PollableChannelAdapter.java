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

package org.springframework.integration.channel;

import java.util.List;

import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * Channel Adapter implementation for a {@link PollableSource}.
 * 
 * @author Mark Fisher
 */
public class PollableChannelAdapter extends AbstractChannelAdapter implements PollableChannel, MessageDeliveryAware {

	private final PollableSource<?> source;


	public PollableChannelAdapter(String name, PollableSource<?> source, MessageTarget target) {
		super(name, target);
		this.source = source;
	}


	public Message<?> receive() {
		return this.receive(-1);
	}

	public Message<?> receive(long timeout) {
		if (this.source != null) {
			return (timeout >= 0 && this.source instanceof BlockingSource)
					? ((BlockingSource<?>) this.source).receive(timeout)
					: this.source.receive();
		}
		return null;
	}

	public List<Message<?>> clear() {
		if (this.source != null && this.source instanceof PollableChannel) {
			return ((PollableChannel) this.source).clear();
		}
		return null;
	}

	public List<Message<?>> purge(MessageSelector selector) {
		if (this.source != null && this.source instanceof PollableChannel) {
			return ((PollableChannel) this.source).purge(selector);
		}
		return null;
	}

	public void onSend(Message<?> sentMessage) {
		if (this.source != null && this.source instanceof MessageDeliveryAware) {
			((MessageDeliveryAware) this.source).onSend(sentMessage);
		}
	}

	public void onFailure(MessagingException exception) {
		if (this.source != null && this.source instanceof MessageDeliveryAware) {
			((MessageDeliveryAware) this.source).onFailure(exception);
		}
	}

}
