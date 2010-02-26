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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.adapter.MessageMappingException;

/**
 * A non-blocking SocketReader that reads from a {@link java.nio.channels.SocketChannel}.
 * 
 * @author Gary Russell
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

	protected int receiveBufferSize = 1024 * 60;

	protected boolean building;

	/**
	 * Constructs an NioSocketReader which reads from the SocketChannel.
	 * @param channel The channel.
	 */
	public NioSocketReader(SocketChannel channel) {
		this.channel = channel;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#read(java.nio.ByteBuffer, int)
	 */
	public byte[] getAssembledData() {
		byte[] assembledData = this.assembledData;
		this.assembledData = null;
		return assembledData;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#assembleData()
	 */
	@Override
	public boolean assembleDataLengthFormat() throws IOException {
		if (lengthPart == null) {
			lengthPart = allocate(4);
		}
		if (lengthPart.hasRemaining()) {
			readChannel(lengthPart);
			return false;
		}
		if (dataPart == null) {
			lengthPart.flip();
			int messageLength = lengthPart.getInt();
			if (logger.isDebugEnabled()) {
				logger.debug("Message length is " + messageLength);
			}
			dataPart = allocate(messageLength);
		}
		if (dataPart.hasRemaining()) {
			readChannel(dataPart);
			if (dataPart.hasRemaining()) {
				return false;
			}
		} 
		if (usingDirectBuffers) {
			byte[] assembledData = new byte[dataPart.capacity()];
			dataPart.flip();
			dataPart.get(assembledData);
			this.assembledData = assembledData;
		} else {
			assembledData = dataPart.array();
		}
		lengthPart = dataPart = null;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataStxEtxFormat()
	 */
	@Override
	protected boolean assembleDataStxEtxFormat() throws IOException {
		if (readChannelNonDeterministic()) {
			byte bite = rawBuffer.get();
			int count = 0;
			if (!building) { 
				if (bite != STX) {
					throw new MessageMappingException("Expected STX, received " + Integer.toHexString(bite));
				}
				building = true;
				count++;
				if (!rawBuffer.hasRemaining()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Incomplete message, consumed 1 byte");
					}
					return false;
				}
			} else {
				if (bite == ETX) {
					finishAssembly();
					return true;
				}
				buildBuffer.put(bite);
				count++;
			}
			while (true) {
				if (!rawBuffer.hasRemaining()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Incomplete message, consumed " + count + " bytes");
					}
					return false;
				}
				bite = rawBuffer.get();
				if (bite == ETX) {
					break;
				}
				buildBuffer.put(bite);
				count++;
			} 
			if (logger.isDebugEnabled()) {
				logger.debug("Consumed " + count + " bytes");
			}
			finishAssembly();
			return true;
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Incomplete message, consumed 0 bytes");
			}
		}
		return false;
	}

	/**
	 * 
	 */
	private void finishAssembly() {
		assembledData = new byte[buildBuffer.position()];
		System.arraycopy(buildBuffer.array(), 0, assembledData, 0, assembledData.length);
		building = false;
		buildBuffer.clear();
		logger.debug("Message assembly complete");
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCrLfFormat()
	 */
	@Override
	protected boolean assembleDataCrLfFormat() throws IOException {
		if (readChannelNonDeterministic()) {
			int count = 0;
			while (true) {
				if (!rawBuffer.hasRemaining()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Incomplete message, consumed " + count + " bytes");
					}
					return false;
				}
				byte bite = rawBuffer.get();
				if (bite == '\n' && buildBuffer.position() > 0) {
					buildBuffer.position(buildBuffer.position() - 1);
					if (buildBuffer.get() == '\r') {
						buildBuffer.position(buildBuffer.position() - 1);
						break;
					}
				}
				buildBuffer.put(bite);
				count++;
			} 
			if (logger.isDebugEnabled()) {
				logger.debug("Consumed " + count + " bytes");
			}
			finishAssembly();
			return true;
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Incomplete message, consumed 0 bytes");
			}
		}
		return false;
	}

	/**
	 * Throws {@link UnsupportedOperationException}; custom implementations can
	 * subclass this class and provide an implementation.
	 * @throws IOException 
	 * @see org.springframework.integration.ip.tcp.AbstractSocketReader#assembleDataCustomFormat().
	 * 
	 */
	@Override
	protected boolean assembleDataCustomFormat() throws IOException {
		throw new UnsupportedOperationException("Need to subclass for this format");
	}

	/**
	 * Reads from the channel into the buffer. Reads as much data as is 
	 * currently available in the channel.
	 * @param buffer
	 * @throws IOException
	 */
	protected void readChannel(ByteBuffer buffer) throws IOException {
		try {
			int len = channel.read(buffer);
			if (len < 0) {
				logger.debug("Socket closed");
				throw new IOException("Socket closed");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Read " + len + " bytes, buffer is now at " + 
							 buffer.position() + " of " +
							 buffer.capacity());
			}
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * Reads data into the rawBuffer for non-deterministic algorithms.
	 * @return true If data is available.
	 * @throws IOException
	 */
	protected boolean readChannelNonDeterministic() throws IOException {
		if (rawBuffer == null) {
			rawBuffer = allocate(receiveBufferSize);
			buildBuffer = ByteBuffer.allocate(receiveBufferSize);
		} else if (rawBuffer.hasRemaining()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Raw buffer has " + rawBuffer.remaining() + " remaining");
			}
			return true;
		}
		rawBuffer.clear();
		int len = channel.read(rawBuffer);
		if (len == 0) {
			return false;
		}
		rawBuffer.flip();
		if (logger.isDebugEnabled()) {
			logger.debug("Read " + rawBuffer.limit() + " into raw buffer");
		}
		return true;
	}
	
	/**
	 * Allocates a ByteBuffer of the requested length using normal or
	 * direct buffers, depending on the usingDirectBuffers field.
	 * @param length 
	 * @return
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
		} catch (IOException e) {}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.SocketReader#getAddress()
	 */
	public InetAddress getAddress() {
		return this.channel.socket().getInetAddress();
	}
	
	/**
	 * @return the useDirectBuffers
	 */
	public boolean isUsingDirectBuffers() {
		return usingDirectBuffers;
	}

	/**
	 * @param useDirectBuffers the useDirectBuffers to set
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

}
