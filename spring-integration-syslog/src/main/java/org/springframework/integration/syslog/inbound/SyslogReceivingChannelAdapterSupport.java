/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.syslog.inbound;

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

	private MessageConverter converter = new DefaultMessageConverter();

	private boolean converterSet;

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
		if (!this.converterSet) {
			((DefaultMessageConverter) this.converter).setBeanFactory(this.getBeanFactory());
		}
	}

	protected void convertAndSend(Message<?> message) {
		try {
			if (message instanceof ErrorMessage) {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("Error on syslog socket:" + ((ErrorMessage) message).getPayload().getMessage());
				}
			}
			else {
				this.sendMessage(this.converter.fromSyslog(message));
			}
		}
		catch (Exception e) {
			throw new MessagingException(message, "Failed to send Message", e);
		}
	}

}
