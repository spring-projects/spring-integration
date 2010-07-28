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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.mapping.MessageMappingException;

/**
 * A non-blocking SocketReader that reads from a {@link java.nio.channels.SocketChannel}.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class NioSocketReader extends AbstractSocketReader {

	protected final Log logger = LogFactory.getLog(getClass());
	
	protected SocketChannel channel;
	
	protected boolean usingDirectBuffers;
	
	protected ByteBuffer lengthPart;
	
	protected ByteBuffer dataPart;
	
	protected ByteBuffer rawBuffer;
	
	protected ByteBuffer buildBuffer;

	protected boolean building;

	/**
	 * Constructs an NioSocketReader which reads from the SocketChannel.
	 * @param channel The channel.
	 */
	public NioSocketReader(SocketChannel channel) {
		this.channel = channel;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#assembleData()
	 */
	@Override
	public int assembleDataLengthFormat() throws IOException {
		if (lengthPart == null) {
			lengthPart = allocate(4);
		}
		if (lengthPart.hasRemaining()) {
			int status = readChannel(lengthPart);
			if (status < 0) {
				if (lengthPart.remaining() == 4) {
					// not in the middle of a message, clean close
					return status;
				}
				throw new IOException("Channel closed");
			}
			return MESSAGE_INCOMPLETE;
		}
		if (dataPart == null) {
			lengthPart.flip();
			int messageLength = lengthPart.getInt();
			if (logger.isDebugEnabled()) {
				logger.debug("Message length is " + messageLength);
			}
			if (messageLength > maxMessageSize) {
				throw new IOException("Message length " + messageLength + 
						" exceeds max message length " + maxMessageSize);
			}
			dataPart = ByteBuffer.allocate(messageLength);
		}
		if (dataPart.hasRemaining()) {
			int status = readChannel(dataPart);
			if (status < 0) {
				throw new IOException("Channel closed");
			}
			if (dataPart.hasRemaining()) {
				return MESSAGE_INCOMPLETE;
			}
		} 
		assembledData = dataPart.array();
		lengthPart = dataPart = null;
		return MESSAGE_COMPLETE;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataStxEtxFormat()
	 */
	@Override
	protected int assembleDataStxEtxFormat() throws IOException {
		int len = readChannelNonDeterministic();
		if (len > 0) {
			byte bite = this.rawBuffer.get();
			int count = 0;
			if (!this.building) { 
				if (bite != STX) {
					throw new MessageMappingException("Expected STX, received " + Integer.toHexString(bite));
				}
				this.building = true;
				count++;
				if (!this.rawBuffer.hasRemaining()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Incomplete message, consumed 1 byte");
					}
					return MESSAGE_INCOMPLETE;
				}
			} else {
				if (bite == ETX) {
					finishAssembly();
					return MESSAGE_COMPLETE;
				}
				this.buildBuffer.put(bite);
				count++;
				if (this.buildBuffer.position() >= this.buildBuffer.limit()) {
					throw new IOException("ETX not found before max message length: "
							+ maxMessageSize);
				}
			}
			while (true) {
				if (!this.rawBuffer.hasRemaining()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Incomplete message, consumed " + count + " bytes");
					}
					return MESSAGE_INCOMPLETE;
				}
				bite = this.rawBuffer.get();
				if (bite == ETX) {
					break;
				}
				this.buildBuffer.put(bite);
				count++;
				if (this.buildBuffer.position() >= this.buildBuffer.limit()) {
					throw new IOException("ETX not found before max message length: "
							+ maxMessageSize);
				}
			} 
			if (logger.isDebugEnabled()) {
				logger.debug("Consumed " + count + " bytes");
			}
			finishAssembly();
			return MESSAGE_COMPLETE;
		} else if (len == 0) {
			logger.debug("Incomplete message, nothing to read");
			return MESSAGE_INCOMPLETE;
		} else {
			logger.debug("Channel closed");
			if (!this.building) {
				// not in the middle of a message, clean close
				return SOCKET_CLOSED;
			}
			this.building = false;
			throw new IOException("Channel closed");
		}
	}

	/**
	 * 
	 */
	private void finishAssembly() {
		byte[] assembledData = new byte[buildBuffer.position()];
		System.arraycopy(this.buildBuffer.array(), 0, assembledData, 0, assembledData.length);
		this.building = false;
		this.buildBuffer.clear();
		this.assembledData = assembledData;
		logger.debug("Message assembly complete");
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCrLfFormat()
	 */
	@Override
	protected int assembleDataCrLfFormat() throws IOException {
		int len = readChannelNonDeterministic();
		if (len > 0) {
			int count = 0;
			while (true) {
				if (!rawBuffer.hasRemaining()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Incomplete message, consumed " + count + " bytes");
					}
					return MESSAGE_INCOMPLETE;
				}
				byte bite = rawBuffer.get();
				this.building = true;
				if (bite == '\n' && this.buildBuffer.position() > 0) {
					this.buildBuffer.position(this.buildBuffer.position() - 1);
					if (this.buildBuffer.get() == '\r') {
						this.buildBuffer.position(this.buildBuffer.position() - 1);
						break;
					}
				}
				this.buildBuffer.put(bite);
				count++;
				if (this.buildBuffer.position() >= this.buildBuffer.limit()) {
					throw new IOException("CRLF not found before max message length: "
							+ this.maxMessageSize);
				}
			} 
			if (logger.isDebugEnabled()) {
				logger.debug("Consumed " + count + " bytes");
			}
			finishAssembly();
			return MESSAGE_COMPLETE;
		} else if (len == 0) {
			logger.debug("Incomplete message, nothing to read");
			return MESSAGE_INCOMPLETE;
		} else {
			logger.debug("Channel closed");
			if (!this.building) {	
				// not in the middle of a message, clean close
				return SOCKET_CLOSED;
			}
			this.building = false;
			throw new IOException("Channel closed");
		}
	}

	/**
	 * Throws {@link UnsupportedOperationException}; Java serialization is currently only
	 * supported using the NetSocketReader.
	 * @throws IOException 
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCustomFormat
	 * 
	 */
	protected int assembleDataSerializedFormat() throws IOException {
		throw new UnsupportedOperationException("Serializable not supported using NIO");
	}


	/**
	 * Throws {@link UnsupportedOperationException}; custom implementations can
	 * subclass this class and provide an implementation.
	 * @throws IOException 
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCustomFormat
	 * 
	 */
	@Override
	protected int assembleDataCustomFormat() throws IOException {
		throw new UnsupportedOperationException("Need to subclass for this format");
	}

	/**
	 * Reads from the channel into the buffer. Reads as much data as is 
	 * currently available in the channel.
	 * @param buffer
	 * @throws IOException
	 */
	protected int readChannel(ByteBuffer buffer) throws IOException {
		try {
			int len = channel.read(buffer);
			if (len < 0) {
				logger.debug("Socket closed");
				return len;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Read " + len + " bytes, buffer is now at " + 
							 buffer.position() + " of " +
							 buffer.capacity());
			}
			return len;
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * Reads data into the rawBuffer for non-deterministic algorithms.
	 * @return bytes remaining in raw buffer or < 0 if channel closed
	 * @throws IOException
	 */
	protected int readChannelNonDeterministic() throws IOException {
		if (rawBuffer == null) {
			rawBuffer = allocate(maxMessageSize);
			buildBuffer = ByteBuffer.allocate(maxMessageSize);
		} else if (rawBuffer.hasRemaining()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Raw buffer has " + rawBuffer.remaining() + " remaining");
			}
			return rawBuffer.remaining();
		}
		rawBuffer.clear();
		int len = channel.read(rawBuffer);
		if (len < 0) {
			return len;
		}
		rawBuffer.flip();
		if (logger.isDebugEnabled()) {
			logger.debug("Read " + rawBuffer.limit() + " into raw buffer");
		}
		return rawBuffer.remaining();
	}
	
	/**
	 * Allocates a ByteBuffer of the requested length using normal or
	 * direct buffers, depending on the usingDirectBuffers field.
	 */
	protected ByteBuffer allocate(int length) {
		ByteBuffer buffer;
		if (usingDirectBuffers) {
			buffer = ByteBuffer.allocateDirect(length);
		} else {
			buffer = ByteBuffer.allocate(length);
		}
		return buffer;
	}
	
	

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#doClose()
	 */
	@Override
	protected void doClose() {
		try {
			channel.close();
		} catch (IOException e) {
			logger.error("Error on close", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#getAddress()
	 */
	public InetAddress getAddress() {
		return this.channel.socket().getInetAddress();
	}
	
	public boolean isUsingDirectBuffers() {
		return usingDirectBuffers;
	}

	/**
	 * @param usingDirectBuffers the usingDirectBuffers to set
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#getSocket()
	 */
	public Socket getSocket() {
		return channel.socket();
	}

}
