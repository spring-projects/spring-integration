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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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
 * @since 2.0
 *
 */
public class NetSocketReader extends AbstractSocketReader {

	protected final Log logger = LogFactory.getLog(getClass());
	
	protected Socket socket;
	
	protected ObjectInputStream objectInputStream;

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
	protected int assembleDataLengthFormat() throws IOException {
		byte[] lengthPart = new byte[4];
		int status = read(lengthPart, true);
		if (status < 0) {
			return status;
		}
		int messageLength = ByteBuffer.wrap(lengthPart).getInt();
		if (logger.isDebugEnabled()) {
			logger.debug("Message length is " + messageLength);
		}	
		if (messageLength > this.maxMessageSize) {
			throw new IOException("Message length " + messageLength + 
					" exceeds max message length: " + this.maxMessageSize);
		}
		byte[] messagePart = new byte[messageLength];
		read(messagePart, false);
		this.assembledData = messagePart;
		return MESSAGE_COMPLETE;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataStxEtxFormat()
	 */
	@Override
	protected int assembleDataStxEtxFormat() throws IOException {
		InputStream inputStream = socket.getInputStream();
		int bite = inputStream.read();
		if (bite < 0) {
			return bite;
		}
		if (bite != STX)
			throw new MessageMappingException("Expected STX to begin message");
		byte[] buffer = new byte[this.maxMessageSize];
		int n = 0;
		while ((bite = inputStream.read()) != ETX) {
			checkClosure(bite);
			buffer[n++] = (byte) bite;
			if (n >= this.maxMessageSize) {
				throw new IOException("ETX not found before max message length: "
						+ this.maxMessageSize);
			}
		}
		this.assembledData = new byte[n];
		System.arraycopy(buffer, 0, this.assembledData, 0, n);
		return MESSAGE_COMPLETE;
	}

	private void checkClosure(int bite) throws IOException {
		if (bite < 0) {
			logger.debug("Socket closed");				
			throw new IOException("Socket closed");
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCrLfFormat()
	 */
	@Override
	protected int assembleDataCrLfFormat() throws IOException {
		InputStream inputStream = socket.getInputStream();
		byte[] buffer = new byte[this.maxMessageSize];
		int n = 0;
		int bite;
		while (true) {
			bite = inputStream.read();
			if (bite < 0 && n == 0) {
				return bite;
			}
			checkClosure(bite);
			if (n > 0 && bite == '\n' && buffer[n-1] == '\r')
				break;
			buffer[n++] = (byte) bite;
			if (n >= this.maxMessageSize) {
				throw new IOException("CRLF not found before max message length: "
						+ this.maxMessageSize);
			}
		};
		this.assembledData = new byte[n-1];
		System.arraycopy(buffer, 0, this.assembledData, 0, n-1);
		return MESSAGE_COMPLETE;
	}

	@Override
	protected int assembleDataSerializedFormat() throws IOException {
		try {
			if (this.objectInputStream == null) {
				InputStream is = this.socket.getInputStream();
				this.objectInputStream = new ObjectInputStream(is);
			}
			this.assembledData = this.objectInputStream.readObject();
		} catch (EOFException ee) {
			return SOCKET_CLOSED;
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
		return SocketReader.MESSAGE_COMPLETE;
	}

	/**
	 * Throws {@link UnsupportedOperationException}; custom implementations can
	 * subclass this class and provide an implementation for this method.
	 * @throws IOException 
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCustomFormat().
	 * 
	 */
	@Override
	protected int assembleDataCustomFormat() throws IOException {
		throw new UnsupportedOperationException("Need to subclass for this format");
	}

	/**
	 * Reads data from the socket and puts the data in buffer. Blocks until
	 * buffer is full or a socket timeout occurs.
	 * @param buffer
	 * @param header true if we are reading the header
	 * @return < 0 if socket closed and not in the middle of a message
	 * @throws IOException
	 */
	protected int read(byte[] buffer, boolean header) throws IOException {
		int lengthRead = 0;
		int needed = buffer.length;
		while (lengthRead < needed) {
			int len;
			len = this.socket.getInputStream().read(buffer, lengthRead,
					needed - lengthRead);
			if (len < 0 && header && lengthRead == 0) {
				return len;
			}
			if (len < 0)
				logger.debug("socket closed after " + lengthRead + " of " + needed);
			checkClosure(len);
			lengthRead += len;
			if (logger.isDebugEnabled()) {
				logger.debug("Read " + len + " bytes, buffer is now at " + 
							 lengthRead + " of " +
							 needed);
			}
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#doClose()
	 */
	@Override
	protected void doClose() {
		try {
			socket.close();
		} catch (IOException e) {
			logger.error("Error on close", e);
		}
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
