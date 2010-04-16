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

import java.lang.reflect.Constructor;
import java.nio.channels.SocketChannel;

import org.springframework.beans.BeanUtils;


/**
 * @author Gary Russell
 *
 */
public class TcpNioSendingMessageHandler extends
		AbstractTcpSendingMessageHandler {

	protected volatile SocketChannel socketChannel;

	protected boolean usingDirectBuffers;

	protected Class<NioSocketWriter> customSocketWriter;

	protected int buffsPerConnection = 5;
	
	/**
	 * @param host
	 * @param port
	 */
	public TcpNioSendingMessageHandler(String host, int port) {
		super(host, port);
	}
	
	/**
	 * @return the socket
	 */
	protected synchronized SocketWriter getWriter() {
		if (socketChannel == null) {
			try {
				socketChannel = SocketChannel.open(this.destinationAddress);
				this.setSocketAttributes(socketChannel.socket());
				NioSocketWriter writer;
				if (messageFormat == MessageFormats.FORMAT_CUSTOM){
					Constructor<NioSocketWriter> ctor = customSocketWriter
							.getConstructor(SocketChannel.class, int.class, int.class);
					writer = BeanUtils.instantiateClass(ctor, socketChannel, buffsPerConnection, soSendBufferSize);
				} else {
					writer = new NioSocketWriter(socketChannel, buffsPerConnection, soSendBufferSize);
				}
				writer.setMessageFormat(messageFormat);
				writer.setUsingDirectBuffers(usingDirectBuffers);
				this.writer = writer;
			} catch (Exception e) {
				logger.error("Error creating SocketWriter", e);
			}
		}
		return this.writer;
	}

	/**
	 * @param usingDirectBuffers Set true if you wish to use direct buffers
	 * for NIO operations.
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	/**
	 * @param customSocketWriter the customSocketWriter to set
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketWriterClassName(
			String customSocketWriterClassName) throws ClassNotFoundException {
		this.customSocketWriter = (Class<NioSocketWriter>) Class
				.forName(customSocketWriterClassName);
	}

	/**
	 * If direct buffers are being used, sets the max number of 
	 * buffers allowed per connection. Defaults to 5. It is unlikely
	 * this would ever need to be changed. Each buffer is set at the 
	 * soSendBufferSize or, if not set, 2048 bytes.
	 * 
	 * @param buffsPerConnection the buffsPerConnection to set
	 */
	public void setBuffsPerConnection(int buffsPerConnection) {
		this.buffsPerConnection = buffsPerConnection;
	}

}
