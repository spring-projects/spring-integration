/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.syslog.DefaultMessageConverter;
import org.springframework.integration.syslog.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

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

	private volatile boolean converterSet;

	/**
	 * @return The port on which this adapter listens.
	 */
	protected int getPort() {
		return this.port;
	}

	/**
	 * Sets the port on which the adapter listens; default is 514; note that
	 * the RFC does not specify a well known port for TCP; 514 is the well-known
	 * port for UDP. Many admins also use 514 for TCP; see RFC-6587 for more
	 * information about TCP and RFC-3164/5424 for more information about UDP.
	 * @param port The port.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * A {@link MessageConverter} to convert the byte array payload
	 * of the underlying UDP/TCP message to a Spring Integration message
	 * with decoded payload and headers; default is {@link DefaultMessageConverter}.
	 * @param converter The converter.
	 */
	public void setConverter(MessageConverter converter) {
		this.converter = converter;
		this.converterSet = true;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (!converterSet) {
			((DefaultMessageConverter) this.converter).setBeanFactory(this.getBeanFactory());
		}
	}

	protected void convertAndSend(Message<?> message) {
		try {
			if (message instanceof ErrorMessage) {
				if (logger.isDebugEnabled()) {
					logger.debug("Error on syslog socket:" + ((ErrorMessage) message).getPayload().getMessage());
				}
			}
			else {
				this.sendMessage(this.converter.fromSyslog(message));
			}
		}
		catch (Exception e) {
			throw new MessagingException(message, e);
		}
	}

}
