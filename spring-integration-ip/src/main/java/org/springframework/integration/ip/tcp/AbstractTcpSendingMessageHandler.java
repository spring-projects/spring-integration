/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.ip.tcp;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageDeliveryException;
import org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageMappingException;
import org.springframework.integration.message.MessageRejectedException;

/**
 * Abstract class for TCP sending message handlers. Implementations
 * for {@link java.net.Socket} and {@link java.nio.channels.SocketChannel}
 * are provided.
 * 
 * @author Gary Russell
 *
 */
public abstract class AbstractTcpSendingMessageHandler extends
		AbstractInternetProtocolSendingMessageHandler {
	
	protected SocketMessageMapper mapper = new SocketMessageMapper();
	
	protected AbstractSocketWriter writer;

	protected boolean soTcpNoDelay = false;
	
	protected int soLinger = -1;
	
	protected int soTrafficClass = -1;
	
	protected boolean soKeepAlive = false;

	protected int messageFormat = MessageFormats.FORMAT_LENGTH_HEADER;
	
	/**
	 * Constructs a message handler that sends messages to the specified
	 * host and port.
	 * @param host The host.
	 * @param port The port.
	 */
	public AbstractTcpSendingMessageHandler(String host, int port) {
		super(host, port);
	}

	/**
	 * Sets socket attributes on the socket.
	 * @param socket The socket.
	 * @throws SocketException
	 */
	protected void setSocketAttributes(Socket socket) throws SocketException {
		if (this.soTimeout >= 0) {
			socket.setSoTimeout(this.soTimeout);
		}
		if (this.soSendBufferSize > 0) {
			socket.setSendBufferSize(this.soSendBufferSize);
		}
		socket.setTcpNoDelay(this.soTcpNoDelay);
		if (soLinger >= 0) {
			socket.setSoLinger(true, this.soLinger);
		}
		if (soTrafficClass >= 0) {
			socket.setTrafficClass(this.soTrafficClass);
		}
		socket.setKeepAlive(this.soKeepAlive);
	}

	/**
	 * Returns the socket writer after instantiating it, if necessary.
	 * @return The writer.
	 */
	protected abstract SocketWriter getWriter();

	/**
	 * Writes the message payload to the underlying socket, using the specified
	 * message format. 
	 * @see org.springframework.integration.message.MessageHandler#handleMessage(org.springframework.integration.core.Message)
	 */
	public void handleMessage(final Message<?> message) throws MessageRejectedException,
			MessageHandlingException, MessageDeliveryException {
		try {
			doWrite(message);
		} catch (MessageMappingException e) {
			// retry - socket may have closed
			if (e.getCause() instanceof IOException) {
				doWrite(message);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Method that actually does the write.
	 * @param message The message to write.
	 */
	protected void doWrite(Message<?> message) {
		try {
			Object object = mapper.fromMessage(message);
			SocketWriter writer = this.getWriter();
			if (writer == null) {
				throw new MessageMappingException(message, "Failed to create SocketWriter");
			}
			writer.write(object);
		} catch (Exception e) {
			this.writer = null;
			if (e instanceof MessageMappingException) {
				throw (MessageMappingException) e;
			}
			throw new MessageMappingException(message, "Failed to map message", e);
		}
	}

	/**
	 * @see Socket#setTcpNoDelay(boolean)
	 * @param soTcpNoDelay the soTcpNoDelay to set
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.soTcpNoDelay = soTcpNoDelay;
	}

	/**
	 * Enables SO_LINGER on the underlying socket.
	 * @see Socket#setSoLinger(boolean, int)
	 * @param soLinger the soLinger to set
	 */
	public void setSoLinger(int soLinger) {
		this.soLinger = soLinger;
	}

	/**
	 * @see Socket#setTrafficClass(int)
	 * @param soTrafficClass the soTrafficClass to set
	 */
	public void setSoTrafficClass(int soTrafficClass) {
		this.soTrafficClass = soTrafficClass;
	}

	/**
	 * @see Socket#setKeepAlive(boolean)
	 * @param soKeepAlive the soKeepAlive to set
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @see MessageFormats
	 * @param messageFormat the messageFormat to set
	 */
	public void setMessageFormat(int messageFormat) {
		this.messageFormat = messageFormat;
		mapper.setMessageFormat(messageFormat);
	}

}
