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
					Constructor<NioSocketWriter> ctor = customSocketWriter.getConstructor(SocketChannel.class);
					writer = BeanUtils.instantiateClass(ctor, socketChannel);
				} else {
					writer = new NioSocketWriter(socketChannel);
				}
				writer.setMessageFormat(messageFormat);
				writer.setUsingDirectBuffers(usingDirectBuffers);
				this.writer = writer;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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

}
