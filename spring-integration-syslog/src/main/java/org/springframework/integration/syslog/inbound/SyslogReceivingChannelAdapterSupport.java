/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.syslog.inbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.syslog.DefaultMessageConverter;
import org.springframework.integration.syslog.MessageConverter;

/**
 * Base support class for inbound channel adapters. The default port is 514.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public abstract class SyslogReceivingChannelAdapterSupport extends MessageProducerSupport {

	protected static final int DEFAULT_PORT = 514;

	private volatile int port = DEFAULT_PORT;

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageConverter converter = new DefaultMessageConverter();

	protected int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setConverter(MessageConverter converter) {
		this.converter = converter;
	}

	protected void convertAndSend(Message<?> message) {
		try {
			this.sendMessage(this.converter.fromSyslog(message));
		}
		catch (Exception e) {
			throw new MessagingException(message, e);
		}
	}

}
