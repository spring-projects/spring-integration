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
 * A {@link SocketWriter} that writes to a {@link java.nio.channels.SocketChannel}. The 
 * data is wrapped in a wire protocol based on the messageFormat property.
 *
 * @author Gary Russell
 *
 */
public class NioSocketWriter extends AbstractSocketWriter {

	protected SocketChannel channel;
	
	/**
	 * If true, direct buffers are used. 
	 * @see {@link ByteBuffer} for more information.
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
	
	/**
	 * @param socket
	 */
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
	 * @param usingDirectBuffers the usingDirectBuffers to set
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	protected ByteBuffer getBuffer() throws InterruptedException  {
		ByteBuffer buffer = this.buffers.poll();
		if (buffer != null) {
			return buffer;
		}
		synchronized (buffers) {
			if (bufferCount < maxBuffers) {
				bufferCount++;
				return ByteBuffer.allocateDirect(this.sendBufferSize);
			}
			// another thread may have returned one while we were sync'd
			buffer = this.buffers.poll();
			if (buffer != null) {
				return buffer;
			}
		}
		buffer = this.buffers.take();
		buffer.clear();
		return buffer;
	}
	
	protected void returnBuffer(ByteBuffer buffer) {
		if (buffer != null) {
			buffers.offer(buffer);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCrLfFormat(byte[])
	 */
	@Override
	protected void writeCrLfFormat(byte[] bytes) throws IOException {
		ByteBuffer buffer = null;
		if (usingDirectBuffers) {
			try {
				checkBufferSize(bytes, 2);
				buffer = getBuffer();
				buffer.put(bytes);
				buffer.put((byte) '\r');
				buffer.put((byte) '\n');
				buffer.flip();
				channel.write(buffer);
				return;
			} catch (InterruptedException e) {
				throw new IOException("Could not get buffer; interrupted");
			} finally {
				returnBuffer(buffer);
			}
		}
		synchronized (channel) {
			if (crLfPart == null) {
				crLfPart = ByteBuffer.allocate(2);
				crLfPart.put((byte) '\r');
				crLfPart.put((byte) '\n');
			}
			channel.write(ByteBuffer.wrap(bytes));
			crLfPart.flip();
			channel.write(crLfPart);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeCustomFormat(byte[])
	 */
	@Override
	protected void writeCustomFormat(byte[] bytes) throws IOException {
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeLengthFormat(byte[])
	 */
	@Override
	protected void writeLengthFormat(byte[] bytes) throws IOException {
		ByteBuffer buffer = null;
		if (usingDirectBuffers) {
			try {
				checkBufferSize(bytes, 4);
				buffer = getBuffer();
				buffer.putInt(bytes.length);
				buffer.put(bytes);
				buffer.flip();
				channel.write(buffer);
				return;
			} catch (InterruptedException e) {
				throw new IOException("Could not get buffer; interrupted");
			} finally {
				returnBuffer(buffer);
			}
		}
		synchronized (channel) {
			if (lengthPart == null) {
				lengthPart = ByteBuffer.allocate(4);
			} else {
				lengthPart.clear();
			}
			lengthPart.putInt(bytes.length);
			lengthPart.flip();
			channel.write(lengthPart);
			channel.write(ByteBuffer.wrap(bytes));
		}		
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.tcp.AbstractSocketWriter#writeStxEtxFormat(byte[])
	 */
	@Override
	protected void writeStxEtxFormat(byte[] bytes) throws IOException {
		ByteBuffer buffer = null;
		if (usingDirectBuffers) {
			try {
				checkBufferSize(bytes, 2);
				buffer = getBuffer();
				buffer.put((byte) STX); 
				buffer.put(bytes);
				buffer.put((byte) ETX);
				buffer.flip();
				channel.write(buffer);
				return;
			} catch (InterruptedException e) {
				throw new IOException("Could not get buffer; interrupted");
			} finally {
				returnBuffer(buffer);
			}
			
		}
		synchronized (channel) {
			if (stxPart == null) {
				stxPart = ByteBuffer.allocate(1);
				stxPart.put((byte) STX);
				etxPart = ByteBuffer.allocate(1);
				etxPart.put((byte) ETX);
			}
			stxPart.flip();
			channel.write(stxPart);
			channel.write(ByteBuffer.wrap(bytes));
			etxPart.flip();
			channel.write(etxPart);
		}
	}

	/**
	 * @param bytes
	 * @throws IOException
	 */
	private void checkBufferSize(byte[] bytes, int pad) throws IOException {
		if (bytes.length + pad > sendBufferSize) {
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
			channel.close();
		} catch (IOException e) {}
	}

	
}
