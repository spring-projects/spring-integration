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
import java.net.InetAddress;
import java.net.Socket;

/**
 * General interface for assembling message data from a TCP/IP Socket.
 * Implementations for {@link java.net.Socket} and {@link java.nio.channels.SocketChannel}
 * are provided.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public interface SocketReader {

	public static int MESSAGE_INCOMPLETE = 0;
	
	public static int MESSAGE_COMPLETE = 1;
	
	/**
	 * Reads the data the socket and assembles 
	 * packets of data into a complete message, depending on the format of that
	 * data.
	 * @return MESSAGE_COMPLETE when message is assembled, otherwise MESSAGE_IMCOMPLETE, or
	 * < 0 if socket closed before any data for a message is received.
	 * @throws IOException 
	 */
	public int assembleData() throws IOException;
	
	/**
	 * Retrieves the assembled tcp data or null if the data is not
	 * yet assembled. Once this method is called, the assembled data is
	 * again null until a new assembly is completed.
	 * @return The assembled data or null.
	 */
	public Object getAssembledData();

	/**
	 * Returns the InetAddress of the underlying socket.
	 * @return The InetAddress.
	 */
	public InetAddress getAddress();

	/**
	 * @return the Socket
	 */
	public Socket getSocket();
}
