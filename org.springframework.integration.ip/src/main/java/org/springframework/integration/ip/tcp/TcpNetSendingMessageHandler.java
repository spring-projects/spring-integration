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
import java.net.Socket;

import javax.net.SocketFactory;

import org.springframework.beans.BeanUtils;


/**
 * TCP Sending Channel Adapter that that uses a {@link java.net.Socket}.
 * @author Gary Russell
 *
 */
public class TcpNetSendingMessageHandler extends
		AbstractTcpSendingMessageHandler {

	protected Class<NetSocketWriter> customSocketWriter;

	/**
	 * Constructs a TcpNetSendingMessageHandler that sends data to the 
	 * specified host and port.
	 * @param host The host.
	 * @param port The port.
	 */
	public TcpNetSendingMessageHandler(String host, int port) {
		super(host, port);
	}
	
	protected volatile Socket socket;
	
	/**
	 * if
	 * @return the writer
	 */
	protected synchronized SocketWriter getWriter() {
		if (writer == null) {
			try {
				this.socket = SocketFactory.getDefault().createSocket(this.host, this.port);
				this.setSocketAttributes(socket);
				NetSocketWriter writer;
				if (messageFormat == MessageFormats.FORMAT_CUSTOM){
					Constructor<NetSocketWriter> ctor = customSocketWriter.getConstructor(Socket.class);
					writer = BeanUtils.instantiateClass(ctor, socket);
				} else {
					writer = new NetSocketWriter(socket);
				}
				writer.setMessageFormat(messageFormat);
				this.writer = writer;
			} catch (Exception e) {
				logger.error("Error creating SocketWriter", e);
			}
		}
		return this.writer;
	}

	/**
	 * @param customSocketWriter the customSocketWriter to set
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketWriterClassName(
			String customSocketWriterClassName) throws ClassNotFoundException {
		this.customSocketWriter = (Class<NetSocketWriter>) Class
				.forName(customSocketWriterClassName);
	}

}
