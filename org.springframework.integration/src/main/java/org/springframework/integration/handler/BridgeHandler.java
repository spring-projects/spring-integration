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

package org.springframework.integration.handler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.Message;
import org.springframework.util.Assert;

/**
 * A simple MessageHandler implementation that passes the request Message
 * directly to the output channel without modifying it. The main purpose of
 * this handler is to bridge a PollableChannel to a SubscribableChannel or
 * vice-versa.
 * 
 * @author Mark Fisher
 */
public class BridgeHandler extends AbstractReplyProducingMessageHandler implements InitializingBean {

	public void afterPropertiesSet() {
		this.verifyOutputChannel();
	}

	@Override
	protected void handleRequestMessage(Message<?> requestMessage, ReplyMessageHolder replyMessageHolder) {
		this.verifyOutputChannel();
		replyMessageHolder.set(requestMessage);
	}

	private void verifyOutputChannel() {
		Assert.state(super.getOutputChannel() != null, "Bridge handler requires an output channel");
	}

}
