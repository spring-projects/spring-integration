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

package org.springframework.integration.dispatcher;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.SubscribableSource;
import org.springframework.integration.message.MessageTarget;

/**
 * A channel that invokes the subscribed {@link MessageTarget target(s)} in
 * the sender's thread (returning after at most one handles the message).
 * 
 * @author Dave Syer
 * @author Mark Fisher
 */
public class DirectChannel extends AbstractMessageChannel implements SubscribableSource {

	private final SimpleDispatcher dispatcher = new SimpleDispatcher();


	public boolean subscribe(MessageTarget target) {
		return this.dispatcher.subscribe(target);
	}

	public boolean unsubscribe(MessageTarget target) {
		return this.dispatcher.unsubscribe(target);
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		return this.dispatcher.send(message);
	}

}
