/*
 * Copyright 2002-2010 the original author or authors
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
import org.springframework.integration.gateway.AbstractMessagingGateway;
import org.springframework.integration.ip.util.SocketIoUtils;
import org.springframework.integration.message.MessageMappingException;

/**
 * Simple implementation of a TCP/IP inbound gateway; uses {@link java.net.Socket}
 * and socket reader thread hangs on receive for response; therefore no multiplexing
 * of incoming messages is supported. Delegates most of its work to a private
 * subclass of {@link TcpNetReceivingChannelAdapter}, overriding the 
 * processMessage() method.
 * 
 * Consequently, the pool size needs to be large enough to support the maximum
 * number of concurrent connections expected.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class SimpleTcpNetInboundGateway extends AbstractMessagingGateway {
	
	protected SocketMessageMapper mapper = new SocketMessageMapper();

	protected WriteCapableTcpNetReceivingChannelAdapter delegate;

	protected int port;

	protected int messageFormat = MessageFormats.FORMAT_LENGTH_HEADER;

	protected int poolSize = 2;

	protected int receiveBufferSize = 2048;

	protected boolean soKeepAlive;

	protected int soReceiveBufferSize = -1;

	protected int soSendBufferSize = -1;

	protected int soTimeout = 0;

	protected String customSocketReaderClassName;
	
	protected Class<NetSocketWriter> customSocketWriterClass;
	
	protected boolean close;
	
	@Override
	protected void doStart() {
		super.doStart();
		this.delegate.start();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.delegate.stop();
	}

	@Override
	protected void onInit() throws Exception {
		this.delegate = new WriteCapableTcpNetReceivingChannelAdapter(port);
		this.delegate.setMessageFormat(messageFormat);
		this.delegate.setPoolSize(poolSize);
		this.delegate.setReceiveBufferSize(receiveBufferSize);
		this.delegate.setSoKeepAlive(soKeepAlive);
		this.delegate.setSoReceiveBufferSize(soReceiveBufferSize);
		this.delegate.setSoSendBufferSize(soSendBufferSize);
		this.delegate.setSoTimeout(soTimeout);
		this.delegate.setTaskScheduler(getTaskScheduler());
		this.delegate.setCustomSocketReaderClassName(customSocketReaderClassName);
		this.delegate.setClose(close);
		super.onInit();
	}

	@Override
	protected Object fromMessage(Message<?> message) {
		throw new MessageMappingException("Cannot map a message to an object in this gateway");
	}

	@Override
	protected Message<?> toMessage(Object object) {
		try {
			return this.mapper.toMessage((SocketReader) object);
		} catch (Exception e) {
			throw new MessageMappingException("Failed to map message", e);
		}
	}
	
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @param messageFormat the messageFormat to set
	 */
	public void setMessageFormat(int messageFormat) {
		this.messageFormat = messageFormat;
	}

	/**
	 * @param poolSize the poolSize to set
	 */
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	/**
	 * @param receiveBufferSize the receiveBufferSize to set
	 */
	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	/**
	 * @param soKeepAlive the soKeepAlive to set
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * @param soReceiveBufferSize the soReceiveBufferSize to set
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @param soSendBufferSize the soSendBufferSize to set
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}

	/**
	 * @param soTimeout the soTimeout to set
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @param customSocketReaderClassName the customSocketReaderClassName to set
	 */
	public void setCustomSocketReaderClassName(String customSocketReaderClassName) {
		this.customSocketReaderClassName = customSocketReaderClassName;
	}

	/**
	 * @param customSocketWriterClassName the customSocketWriterClassName to set
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketWriterClassName(
			String customSocketWriterClassName) throws ClassNotFoundException {
		if (customSocketWriterClassName != null) {
			this.customSocketWriterClass = (Class<NetSocketWriter>) Class
				.forName(customSocketWriterClassName);
			if (!(NetSocketWriter.class.isAssignableFrom(this.customSocketWriterClass))) {
				throw new IllegalArgumentException("Custom socket writer must be of type NetSocketWriter");
			}
		}
	}

	/**
	 * @param close the close to set
	 */
	public void setClose(boolean close) {
		this.close = close;
	}
	
	public boolean isListening() {
		return delegate.isListening();
	}

	private class WriteCapableTcpNetReceivingChannelAdapter extends TcpNetReceivingChannelAdapter {

		/**
		 * @param port
		 */
		public WriteCapableTcpNetReceivingChannelAdapter(int port) {
			super(port);
		}

		@Override
		protected void processMessage(NetSocketReader reader) {
			Socket socket = reader.getSocket();
			Message<?> message = sendAndReceiveMessage(reader);
			NetSocketWriter writer = SocketIoUtils.createNetWriter(this.messageFormat, 
					customSocketWriterClass, socket);
			try {
				writer.write(this.mapper.fromMessage(message));
				if (close) {
					try {
						socket.close();
					} catch (IOException ioe) {
						logger.error("Error on close", ioe);
					}
				}
			} catch (Exception e) {
				throw new MessageMappingException("Failed to map and send response", e);
			}
		}

		@Override
		protected void doStart() {
;			super.doStart();
		}

		@Override
		protected void setSocketOptions(Socket socket) throws SocketException {
			super.setSocketOptions(socket);
			if (soSendBufferSize > 0) {
				socket.setSendBufferSize(soSendBufferSize);
			}
		}
		
	}
}
