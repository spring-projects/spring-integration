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

import java.lang.reflect.Constructor;
import java.net.Socket;
import java.net.SocketException;

import org.springframework.beans.BeanUtils;
import org.springframework.integration.core.Message;
import org.springframework.integration.gateway.AbstractMessagingGateway;
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
 *
 */
public class SimpleTcpNetInboundGateway extends AbstractMessagingGateway {
	
	private SocketMessageMapper mapper = new SocketMessageMapper();

	private WriteCapableTcpNetReceivingChannelAdapter delegate;

	private int port;

	private int messageFormat = MessageFormats.FORMAT_LENGTH_HEADER;

	private int poolSize = 2;

	private int receiveBufferSize = 2048;

	private boolean soKeepAlive;

	private int soReceiveBufferSize = -1;

	private int soSendBufferSize = -1;

	private int soTimeout = 0;

	private String customSocketReaderClassName;
	
	private Class<NetSocketWriter> customSocketWriter;
	
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.gateway.AbstractMessagingGateway#doStart()
	 */
	@Override
	protected void doStart() {
		super.doStart();
		delegate.start();
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.gateway.AbstractMessagingGateway#doStop()
	 */
	@Override
	protected void doStop() {
		super.doStop();
		delegate.stop();
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.gateway.AbstractMessagingGateway#onInit()
	 */
	@Override
	protected void onInit() throws Exception {
		delegate = new WriteCapableTcpNetReceivingChannelAdapter(port);
		delegate.setMessageFormat(messageFormat);
		delegate.setPoolSize(poolSize);
		delegate.setReceiveBufferSize(receiveBufferSize);
		delegate.setSoKeepAlive(soKeepAlive);
		delegate.setSoReceiveBufferSize(soReceiveBufferSize);
		delegate.setSoSendBufferSize(soSendBufferSize);
		delegate.setSoTimeout(soTimeout);
		delegate.setTaskScheduler(getTaskScheduler());
		delegate.setCustomSocketReaderClassName(customSocketReaderClassName);
		super.onInit();
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.gateway.AbstractMessagingGateway#fromMessage(org.springframework.integration.core.Message)
	 */
	@Override
	protected Object fromMessage(Message<?> message) {
		throw new MessageMappingException("Cannot map a message to an object in this gateway");
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.gateway.AbstractMessagingGateway#toMessage(java.lang.Object)
	 */
	@Override
	protected Message<?> toMessage(Object object) {
		try {
			return mapper.toMessage((SocketReader) object);
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
		return port;
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
	 * @param customSocketWriter the customSocketWriter to set
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketWriterClassName(
			String customSocketWriterClassName) throws ClassNotFoundException {
		if (customSocketWriterClassName != null) {
			this.customSocketWriter = (Class<NetSocketWriter>) Class
				.forName(customSocketWriterClassName);
			if (!(NetSocketWriter.class.isAssignableFrom(this.customSocketWriter))) {
				throw new IllegalArgumentException("Custom socket writer must be of type NetSocketWriter");
			}
		}
	}

	private class WriteCapableTcpNetReceivingChannelAdapter extends TcpNetReceivingChannelAdapter {

		/**
		 * @param port
		 */
		public WriteCapableTcpNetReceivingChannelAdapter(int port) {
			super(port);
		}

		/* (non-Javadoc)
		 * @see org.springframework.integration.ip.tcp.TcpNetReceivingChannelAdapter#processMessage(org.springframework.integration.core.Message)
		 */
		@Override
		protected void processMessage(NetSocketReader reader) {
			Socket socket = reader.getSocket();
			NetSocketWriter writer = null;
			try {
				if (messageFormat == MessageFormats.FORMAT_CUSTOM){
					Constructor<NetSocketWriter> ctor = customSocketWriter.getConstructor(Socket.class);
					writer = BeanUtils.instantiateClass(ctor, socket);
				} else {
					writer = new NetSocketWriter(socket);
				}
			} catch (Exception e) {
				throw new MessageMappingException("Error creating SocketWriter", e);
			}
			writer.setMessageFormat(messageFormat);
			Message<?> message = sendAndReceiveMessage(reader);
			try {
				writer.write(mapper.fromMessage(message));
			} catch (Exception e) {
				throw new MessageMappingException("Failed to map and send response", e);
			}
		}

		/* (non-Javadoc)
		 * @see org.springframework.integration.ip.AbstractInternetProtocolReceivingChannelAdapter#doStart()
		 */
		@Override
		protected void doStart() {
;			super.doStart();
		}

		/* (non-Javadoc)
		 * @see org.springframework.integration.ip.tcp.AbstractTcpReceivingChannelAdapter#setSocketOptions(java.net.Socket)
		 */
		@Override
		protected void setSocketOptions(Socket socket) throws SocketException {
			super.setSocketOptions(socket);
			if (soSendBufferSize > 0) {
				socket.setSendBufferSize(soSendBufferSize);
			}
		}
		
	}
}
