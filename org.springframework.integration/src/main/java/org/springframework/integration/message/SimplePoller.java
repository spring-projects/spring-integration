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

package org.springframework.integration.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Mark Fisher
 */
public class SimplePoller implements Poller {

	private final Log logger = LogFactory.getLog(this.getClass());

	private long receiveTimeout = 5000;

	private long sendTimeout = -1;


	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public int poll(MessageSource<?> source, MessageTarget target) {
		Message<?> message = (source instanceof BlockingSource && this.receiveTimeout >= 0) ?
				((BlockingSource<?>) source).receive(this.receiveTimeout) : source.receive();
		if (message == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("received no Message from source '" + source + "'");
			}
			return 0;
		}
		boolean sent = (target instanceof BlockingTarget && this.sendTimeout >= 0) ?
				((BlockingTarget) target).send(message, this.sendTimeout) : target.send(message);
		if (source instanceof MessageDeliveryAware) {
			if (sent) {
				((MessageDeliveryAware) source).onSend(message);
			}
			else {
				((MessageDeliveryAware) source).onFailure(new MessageDeliveryException(message, "failed to send message"));
			}
		}
		return (sent ? 1 : 0);
	}

}
