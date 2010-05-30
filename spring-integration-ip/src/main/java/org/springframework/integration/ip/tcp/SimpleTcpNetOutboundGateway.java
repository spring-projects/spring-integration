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

import java.net.Socket;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.ip.util.SocketIoUtils;

/**
 * Simple TCP outbound gateway; delegates write to a {@link TcpNetSendingMessageHandler}
 * then blocks on read of same socket. Uses {@link java.net.Socket} and the client 
 * thread is dedicated to a request/response pair. No multiplexing of requests
 * over the outbound socket are supported. This class is thread safe in that
 * if multiple clients attempt to send a message, they will be blocked until
 * any existing request/response is processed.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class SimpleTcpNetOutboundGateway extends
		AbstractReplyProducingMessageHandler {

	
	protected TcpNetSendingMessageHandler handler;

	protected Class<NetSocketReader> customSocketReaderClass;
	
	protected int messageFormat;
	
	protected int maxMessageSize = 2048;
	
	protected int soReceiveBufferSize = -1;
	
	protected NetSocketReader reader;
	
	protected boolean close;
	
	/**
	 * Constructs a SimpleTcpNetOutboundGateway that sends data to the 
	 * specified host and port, and waits for a response.
	 * 
	 * @param host The host.
	 * @param port The port.
	 */
	public SimpleTcpNetOutboundGateway(String host, int port) {
		handler = new TcpNetSendingMessageHandler(host, port);
	}
	
	/**
	 * Synchronized to prevent multiplexing requests over the same socket.
	 */
	@Override
	protected synchronized Object handleRequestMessage(Message<?> requestMessage) {
		this.handler.handleMessage(requestMessage);
		Socket socket = this.handler.getSocket();
		if (this.reader == null ||
			this.reader.getSocket() != socket) { // might have re-opened on error
			this.reader = SocketIoUtils.createNetReader(this.messageFormat,
				this.customSocketReaderClass, socket, this.maxMessageSize,
				this.soReceiveBufferSize);
		}
		try {
			this.reader.assembleData();  // Net... always returns true
			Object object = this.reader.getAssembledData();
			if (close) {
				logger.debug("Closing socket because close=true");
				this.handler.close();
			}
			return object;
		} catch (Exception e) {
			this.reader = null;
			this.handler.close();
			throw new MessagingException(requestMessage, e);
		}
	}

	/**
	 * @see java.lang.Object#equals(Object)
	 * @return whether the MessageHandler delegate for this Gateway is equal to the provided object
	 */
	public boolean equals(Object obj) {
		return handler.equals(obj);
	}

	/**
	 * @see org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler#getPort()
	 * @return the port number of the MessageHandler delegate for this Gateway
	 */
	public int getPort() {
		return handler.getPort();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 * @return hashcode value of the MessageHandler delegate for this Gateway
	 */
	public int hashCode() {
		return handler.hashCode();
	}

	/**
	 * @param customSocketWriterClassName
	 * @throws ClassNotFoundException
	 * @see org.springframework.integration.ip.tcp.TcpNetSendingMessageHandler#setCustomSocketWriterClassName(java.lang.String)
	 */
	public void setCustomSocketWriterClassName(
			String customSocketWriterClassName) throws ClassNotFoundException {
		handler.setCustomSocketWriterClassName(customSocketWriterClassName);
	}

	/**
	 * @param messageFormat
	 * @see org.springframework.integration.ip.tcp.AbstractTcpSendingMessageHandler#setMessageFormat(int)
	 */
	public void setMessageFormat(int messageFormat) {
		handler.setMessageFormat(messageFormat);
		this.messageFormat = messageFormat;
	}

	/**
	 * @param soKeepAlive
	 * @see org.springframework.integration.ip.tcp.AbstractTcpSendingMessageHandler#setSoKeepAlive(boolean)
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		handler.setSoKeepAlive(soKeepAlive);
	}

	/**
	 * @param soLinger
	 * @see org.springframework.integration.ip.tcp.AbstractTcpSendingMessageHandler#setSoLinger(int)
	 */
	public void setSoLinger(int soLinger) {
		handler.setSoLinger(soLinger);
	}

	/**
	 * @param size
	 * @see org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler#setSoReceiveBufferSize(int)
	 */
	public void setSoReceiveBufferSize(int size) {
		this.soReceiveBufferSize = size;
	}

	/**
	 * @param size
	 * @see org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler#setSoSendBufferSize(int)
	 */
	public void setSoSendBufferSize(int size) {
		handler.setSoSendBufferSize(size);
	}

	/**
	 * @param soTcpNoDelay
	 * @see org.springframework.integration.ip.tcp.AbstractTcpSendingMessageHandler#setSoTcpNoDelay(boolean)
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		handler.setSoTcpNoDelay(soTcpNoDelay);
	}

	/**
	 * @param timeout
	 * @see org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler#setSoTimeout(int)
	 */
	public void setSoTimeout(int timeout) {
		handler.setSoTimeout(timeout);
	}

	/**
	 * @param soTrafficClass
	 * @see org.springframework.integration.ip.tcp.AbstractTcpSendingMessageHandler#setSoTrafficClass(int)
	 */
	public void setSoTrafficClass(int soTrafficClass) {
		handler.setSoTrafficClass(soTrafficClass);
	}

	/**
	 * @param customSocketReaderClassName the {@link NetSocketReader} class to use
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketReaderClassName(
			String customSocketReaderClassName) throws ClassNotFoundException {
		if (customSocketReaderClassName != null) {
			this.customSocketReaderClass = (Class<NetSocketReader>) Class
				.forName(customSocketReaderClassName);
			if (!(NetSocketReader.class.isAssignableFrom(this.customSocketReaderClass))) {
				throw new IllegalArgumentException("Custom socket reader must be of type NetSocketReader");
			}
		}
	}

	/**
	 * Specify the Spring Integration reply channel. If this property is not
	 * set the gateway will check for a 'replyChannel' header on the request.
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	/**
	 * @param close the close to set
	 */
	public void setClose(boolean close) {
		this.close = close;
	}

}
