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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.MessageMappingException;

/**
 * A SocketReader that reads from a {@link java.net.Socket}. Threads
 * calling {@link NetSocketReader#assembledData} will block until a message
 * is completely assembled.
 * 
 * @author Gary Russell
 *
 */
public class NetSocketReader extends AbstractSocketReader {

	protected final Log logger = LogFactory.getLog(getClass());
	
	protected Socket socket;

	/**
	 * Constructs a NetsocketReader which reads from the Socket.
	 * @param socket The socket.
	 */
	public NetSocketReader(Socket socket) {
		this.socket = socket;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#read(java.nio.ByteBuffer)
	 */
	@Override
	protected boolean assembleDataLengthFormat() throws IOException {
		byte[] lengthPart = new byte[4];
		read(lengthPart);
		int messageLength = ByteBuffer.wrap(lengthPart).getInt();
		if (logger.isDebugEnabled()) {
			logger.debug("Message length is " + messageLength);
		}	
		if (messageLength > maxMessageSize) {
			throw new IOException("Message length " + messageLength + 
					" exceeds max message length: " + maxMessageSize);
		}
		byte[] messagePart = new byte[messageLength];
		read(messagePart);
		assembledData = messagePart;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataStxEtxFormat()
	 */
	@Override
	protected boolean assembleDataStxEtxFormat() throws IOException {
		InputStream inputStream = socket.getInputStream();
		if (inputStream.read() != STX)
			throw new MessageMappingException("Expected STX to begin message");
		byte[] buffer = new byte[maxMessageSize];
		int n = 0;
		int bite;
		while ((bite = inputStream.read()) != ETX) {
			if (bite < 0) {
				logger.debug("Socket closed");				
				throw new IOException("Socket Closed");
			}
			buffer[n++] = (byte) bite;
			if (n >= maxMessageSize) {
				throw new IOException("ETX not found before max message length: "
						+ maxMessageSize);
			}
		}
		assembledData = new byte[n];
		System.arraycopy(buffer, 0, assembledData, 0, n);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCrLfFormat()
	 */
	@Override
	protected boolean assembleDataCrLfFormat() throws IOException {
		InputStream inputStream = socket.getInputStream();
		byte[] buffer = new byte[maxMessageSize];
		int n = 0;
		int bite;
		while (true) {
			bite = inputStream.read();
			if (bite < 0) {
				logger.debug("Socket closed");				
				throw new IOException("Socket Closed");
			}
			if (n > 0 && bite == '\n' && buffer[n-1] == '\r')
				break;
			buffer[n++] = (byte) bite;
			if (n >= maxMessageSize) {
				throw new IOException("CRLF not found before max message length: "
						+ maxMessageSize);
			}
		};
		assembledData = new byte[n-1];
		System.arraycopy(buffer, 0, assembledData, 0, n-1);
		return true;
	}

	/**
	 * Throws {@link UnsupportedOperationException}; custom implementations can
	 * subclass this class and provide an implementation for this method.
	 * @throws IOException 
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCustomFormat().
	 * 
	 */
	@Override
	protected boolean assembleDataCustomFormat() throws IOException {
		throw new UnsupportedOperationException("Need to subclass for this format");
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#getAssembledData()
	 */
	public byte[] getAssembledData() {
		byte[] assembledData = this.assembledData;
		this.assembledData = null;
		return assembledData;
	}

	/**
	 * Reads data from the socket and puts the data in buffer. Blocks until
	 * buffer is full or a socket timeout occurs.
	 * @param buffer
	 * @throws IOException
	 */
	protected void read(byte[] buffer) throws IOException {
		int lengthRead = 0;
		int needed = buffer.length;
		while (lengthRead < needed) {
			int len;
			len = socket.getInputStream().read(buffer, lengthRead,
					needed - lengthRead);
			if (len < 0) {
				logger.debug("Socket closed");				
				throw new IOException("Socket Closed");
			}
			lengthRead += len;
			if (logger.isDebugEnabled()) {
				logger.debug("Read " + len + " bytes, buffer is now at " + 
							 lengthRead + " of " +
							 needed);
			}
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#doClose()
	 */
	@Override
	protected void doClose() {
		try {
			socket.close();
		} catch (IOException e) {}
	}

	
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#getAddress()
	 */
	public InetAddress getAddress() {
		return this.socket.getInetAddress();
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#getSocket()
	 */
	public Socket getSocket() {
		return socket;
	}

}
