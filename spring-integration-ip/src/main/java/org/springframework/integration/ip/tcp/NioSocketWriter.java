/*
 * Copyright 2002-20/10 the original author or authors.
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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A {@link SocketWriter} that writes to a {@link SocketChannel}. The
 * data is wrapped in a wire protocol based on the messageFormat property.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class NioSocketWriter extends AbstractSocketWriter {

	protected SocketChannel channel;

	/**
	 * If true, direct buffers are used.
	 * @see ByteBuffer for more information
	 */
	protected boolean usingDirectBuffers;

	/**
	 * A buffer containing the length part when the messageFormat is
	 * {@link MessageFormats#FORMAT_LENGTH_HEADER}.
	 */
	protected ByteBuffer lengthPart;

	/**
	 * A buffer containing the STX for when the messageFormat is
	 * {@link MessageFormats#FORMAT_STX_ETX}.
	 */
	protected ByteBuffer stxPart;

	/**
	 * A buffer containing the ETX for when the messageFormat is
	 * {@link MessageFormats#FORMAT_STX_ETX}.
	 */
	protected ByteBuffer etxPart;

	/**
	 * A buffer containing the CRLF for when the messageFormat is
	 * {@link MessageFormats#FORMAT_CRLF}.
	 */
	protected ByteBuffer crLfPart;

	/**
	 * If we are using direct buffers, we don't want to churn them using
	 * normal heap management. But, 
	 * because we can have multiple threads writing and we might write in 
	 * chunks, we need a dedicated buffer for each thread; up to a limit.
	 * We handle this with a blocking queue.
	 */
	protected BlockingQueue<ByteBuffer> buffers;

	protected int maxBuffers = 2;

	protected int bufferCount = 0;

	private int sendBufferSize;

	public NioSocketWriter(SocketChannel channel,
	                       int maxBuffers,
	                       int sendBufferSize) {
		this.channel = channel;
		this.maxBuffers = maxBuffers;
		if (sendBufferSize <= 0) {
			sendBufferSize = 2048;
		}
		this.sendBufferSize = sendBufferSize;
		buffers = new LinkedBlockingQueue<ByteBuffer>(maxBuffers);
	}

	/**
	 * @param usingDirectBuffers whether direct buffers are to be used
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	protected ByteBuffer getBuffer() throws InterruptedException  {
		ByteBuffer buffer = this.buffers.poll();
		if (buffer != null) {
			buffer.clear();
			return buffer;
		}
		synchronized (buffers) {
			if (this.bufferCount < this.maxBuffers) {
				bufferCount++;
				return ByteBuffer.allocateDirect(this.sendBufferSize);
			}
		}
		buffer = this.buffers.take();
		buffer.clear();
		return buffer;
	}

	protected void returnBuffer(ByteBuffer buffer) {
		if (buffer != null) {
			this.buffers.offer(buffer);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCrLfFormat(byte[])
	 */
	@Override
	protected void writeCrLfFormat(byte[] bytes) throws IOException {
		ByteBuffer buffer = null;
		if (this.usingDirectBuffers) {
			try {
				checkBufferSize(bytes, 2);
				buffer = getBuffer();
				buffer.put(bytes);
				buffer.put((byte) '\r');
				buffer.put((byte) '\n');
				buffer.flip();
				this.channel.write(buffer);
				return;
			} catch (InterruptedException e) {
				throw new IOException("Could not get buffer; interrupted");
			} finally {
				returnBuffer(buffer);
			}
		}
		synchronized (channel) {
			if (this.crLfPart == null) {
				this.crLfPart = ByteBuffer.allocate(2);
				this.crLfPart.put((byte) '\r');
				this.crLfPart.put((byte) '\n');
			}
			this.channel.write(ByteBuffer.wrap(bytes));
			this.crLfPart.flip();
			this.channel.write(this.crLfPart);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCustomFormat(byte[])
	 */
	@Override
	protected void writeSerializedFormat(Object object) throws IOException {
		throw new UnsupportedOperationException("Serializable not supported using NIO");
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCustomFormat(byte[])
	 */
	@Override
	protected void writeCustomFormat(Object object) throws IOException {
		throw new UnsupportedOperationException("Need to subclass for this format");		
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeLengthFormat(byte[])
	 */
	@Override
	protected void writeLengthFormat(byte[] bytes) throws IOException {
		ByteBuffer buffer = null;
		if (this.usingDirectBuffers) {
			try {
				checkBufferSize(bytes, 4);
				buffer = getBuffer();
				buffer.putInt(bytes.length);
				buffer.put(bytes);
				buffer.flip();
				this.channel.write(buffer);
				return;
			} catch (InterruptedException e) {
				throw new IOException("Could not get buffer; interrupted");
			} finally {
				returnBuffer(buffer);
			}
		}
		synchronized (channel) {
			if (this.lengthPart == null) {
				this.lengthPart = ByteBuffer.allocate(4);
			} else {
				this.lengthPart.clear();
			}
			this.lengthPart.putInt(bytes.length);
			this.lengthPart.flip();
			this.channel.write(this.lengthPart);
			this.channel.write(ByteBuffer.wrap(bytes));
		}		
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeStxEtxFormat(byte[])
	 */
	@Override
	protected void writeStxEtxFormat(byte[] bytes) throws IOException {
		ByteBuffer buffer = null;
		if (this.usingDirectBuffers) {
			try {
				checkBufferSize(bytes, 2);
				buffer = getBuffer();
				buffer.put((byte) STX); 
				buffer.put(bytes);
				buffer.put((byte) ETX);
				buffer.flip();
				this.channel.write(buffer);
				return;
			} catch (InterruptedException e) {
				throw new IOException("Could not get buffer; interrupted");
			} finally {
				returnBuffer(buffer);
			}
		}
		synchronized (channel) {
			if (this.stxPart == null) {
				this.stxPart = ByteBuffer.allocate(1);
				this.stxPart.put((byte) STX);
				this.etxPart = ByteBuffer.allocate(1);
				this.etxPart.put((byte) ETX);
			}
			this.stxPart.flip();
			this.channel.write(this.stxPart);
			this.channel.write(ByteBuffer.wrap(bytes));
			this.etxPart.flip();
			this.channel.write(this.etxPart);
		}
	}

	/**
	 * @param bytes
	 * @throws IOException
	 */
	private void checkBufferSize(byte[] bytes, int pad) throws IOException {
		if (bytes.length + pad > this.sendBufferSize) {
			throw new IOException("Send buffer too small (" + sendBufferSize + 
						") increase so-send-buffer-size to at least " + 
						bytes.length + pad);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#doClose()
	 */
	@Override
	protected void doClose() {
		try {
			this.channel.close();
		} catch (IOException e) {
			logger.error("Error on close", e);
		}
	}

}
