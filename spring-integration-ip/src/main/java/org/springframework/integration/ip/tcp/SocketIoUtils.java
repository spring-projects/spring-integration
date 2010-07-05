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
import java.nio.channels.SocketChannel;

import org.springframework.beans.BeanUtils;
import org.springframework.integration.message.MessageMappingException;

/**
 * @author Gary Russell
 *
 */
public class SocketIoUtils {

	public static NetSocketReader createNetReader(int messageFormat, 
				Class<NetSocketReader> customSocketReaderClass, 
				Socket socket,
				int receiveBufferSize, 
				int soReceiveBufferSize) {
		NetSocketReader reader;
		if (messageFormat == MessageFormats.FORMAT_CUSTOM) {
			try {
				Constructor<NetSocketReader> ctor = 
					customSocketReaderClass.getConstructor(Socket.class);
				reader = BeanUtils.instantiateClass(ctor, socket);
				if (soReceiveBufferSize > 0) {
					socket.setReceiveBufferSize(soReceiveBufferSize);
				}
			} catch (Exception e) {
				throw new MessageMappingException("Failed to instantiate custom reader", e);
			}
		}
		else {
			reader = new NetSocketReader(socket);
		}
		reader.setMessageFormat(messageFormat);
		reader.setMaxMessageSize(receiveBufferSize);
		return reader;
	}

	public static NetSocketWriter createNetWriter(int messageFormat, 
			Class<NetSocketWriter> customSocketWriterClass, Socket socket) {
		NetSocketWriter writer;
		if (messageFormat == MessageFormats.FORMAT_CUSTOM){
			try {
				Constructor<NetSocketWriter> ctor = customSocketWriterClass.getConstructor(Socket.class);
				writer = BeanUtils.instantiateClass(ctor, socket);
			} catch (Exception e) {
				throw new MessageMappingException("Failed to instantiate custom writer", e);
			}
		} else {
			writer = new NetSocketWriter(socket);
		}
		writer.setMessageFormat(messageFormat);
		return writer;
	}

	public static NioSocketReader createNioReader(int messageFormat, 
				Class<NioSocketReader> customSocketReaderClass, 
				SocketChannel channel,
				int receiveBufferSize, 
				int soReceiveBufferSize,
				boolean usingDirectBuffers ) {
		NioSocketReader reader;
		if (messageFormat == MessageFormats.FORMAT_CUSTOM) {
			try {
				Constructor<NioSocketReader> ctor = 
					customSocketReaderClass.getConstructor(SocketChannel.class);
				reader = BeanUtils.instantiateClass(ctor, channel);
				if (soReceiveBufferSize > 0) {
					channel.socket().setReceiveBufferSize(soReceiveBufferSize);
				}
			} catch (Exception e) {
				throw new MessageMappingException("Failed to instantiate custom reader", e);
			}
		}
		else {
			reader = new NioSocketReader(channel);
		}
		reader.setMessageFormat(messageFormat);
		reader.setMaxMessageSize(receiveBufferSize);
		reader.setUsingDirectBuffers(usingDirectBuffers);
		return reader;
	}

	public static NioSocketWriter createNioWriter(int messageFormat, 
			Class<NioSocketWriter> customSocketWriterClass, 
			SocketChannel channel, 
			int maxBuffers, 
			int sendBufferSize, 
			boolean usingDirectBuffers) {
		NioSocketWriter writer;
		if (messageFormat == MessageFormats.FORMAT_CUSTOM){
			try {
				Constructor<NioSocketWriter> ctor = customSocketWriterClass
				.getConstructor(SocketChannel.class, int.class, int.class);
				writer = BeanUtils.instantiateClass(ctor, channel, maxBuffers, sendBufferSize);
			} catch (Exception e) {
				throw new MessageMappingException("Failed to instantiate custom writer", e);
			}
		} else {
			writer = new NioSocketWriter(channel, maxBuffers, sendBufferSize);
		}
		writer.setMessageFormat(messageFormat);
		writer.setUsingDirectBuffers(usingDirectBuffers);
		return writer;
	}

}
