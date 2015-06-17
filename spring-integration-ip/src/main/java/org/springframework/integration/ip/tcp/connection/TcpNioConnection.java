/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSession;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.util.CompositeExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A TcpConnection that uses and underlying {@link SocketChannel}.
 *
 * @author Gary Russell
 * @author John Anderson
 * @since 2.0
 *
 */
public class TcpNioConnection extends TcpConnectionSupport {

	private static final long DEFAULT_PIPE_TIMEOUT = 60000;

	private final SocketChannel socketChannel;

	private final ChannelOutputStream channelOutputStream;

	private final ChannelInputStream channelInputStream = new ChannelInputStream();

	private volatile OutputStream bufferedOutputStream;

	private volatile boolean usingDirectBuffers;

	private volatile CompositeExecutor taskExecutor;

	private volatile ByteBuffer rawBuffer;

	private volatile int maxMessageSize = 60 * 1024;

	private volatile long lastRead;

	private volatile long lastSend;

	private final AtomicInteger executionControl = new AtomicInteger();

	private volatile boolean writingToPipe;

	private volatile CountDownLatch writingLatch;

	private volatile long pipeTimeout = DEFAULT_PIPE_TIMEOUT;

	/**
	 * Constructs a TcpNetConnection for the SocketChannel.
	 * @param socketChannel The socketChannel.
	 * @param server If true, this connection was created as
	 * a result of an incoming request.
	 * @param lookupHost true to perform reverse lookups.
	 * @param applicationEventPublisher The event publisher.
	 * @param connectionFactoryName The name of the connection factory creating this connection.
	 * @throws Exception Any Exception.
	 */
	public TcpNioConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
			ApplicationEventPublisher applicationEventPublisher,
			String connectionFactoryName) throws Exception {
			super(socketChannel.socket(), server, lookupHost, applicationEventPublisher, connectionFactoryName);
		this.socketChannel = socketChannel;
		int receiveBufferSize = socketChannel.socket().getReceiveBufferSize();
		if (receiveBufferSize <= 0) {
			receiveBufferSize = this.maxMessageSize;
		}
		this.channelOutputStream = new ChannelOutputStream();
	}

	public void setPipeTimeout(long pipeTimeout) {
		this.pipeTimeout = pipeTimeout;
	}

	@Override
	public void close() {
		this.setNoReadErrorOnClose(true);
		doClose();
	}

	private void doClose() {
		try {
			channelInputStream.close();
		}
		catch (IOException e) {}
		try {
			this.socketChannel.close();
		}
		catch (Exception e) {}
		super.close();
	}

	@Override
	public boolean isOpen() {
		return this.socketChannel.isOpen();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void send(Message<?> message) throws Exception {
		synchronized(this.socketChannel) {
			if (this.bufferedOutputStream == null) {
				int writeBufferSize = this.socketChannel.socket().getSendBufferSize();
				this.bufferedOutputStream = new BufferedOutputStream(this.getChannelOutputStream(),
						writeBufferSize > 0 ? writeBufferSize : 8192);
			}
			Object object = this.getMapper().fromMessage(message);
			this.lastSend = System.currentTimeMillis();
			try {
				((Serializer<Object>) this.getSerializer()).serialize(object, this.bufferedOutputStream);
				this.bufferedOutputStream.flush();
			}
			catch (Exception e) {
				this.publishConnectionExceptionEvent(new MessagingException(message, e));
				this.closeConnection(true);
				throw e;
			}
			this.afterSend(message);
		}
	}

	@Override
	public Object getPayload() throws Exception {
		return this.getDeserializer().deserialize(this.channelInputStream);
	}

	@Override
	public int getPort() {
		return this.socketChannel.socket().getPort();
	}

	@Override
	public Object getDeserializerStateKey() {
		return this.channelInputStream;
	}

	@Override
	public SSLSession getSslSession() {
		return null;
	}

	/**
	 * Allocates a ByteBuffer of the requested length using normal or
	 * direct buffers, depending on the usingDirectBuffers field.
	 *
	 * @param length The buffer length.
	 * @return The buffer.
	 */
	protected ByteBuffer allocate(int length) {
		ByteBuffer buffer;
		if (this.usingDirectBuffers) {
			buffer = ByteBuffer.allocateDirect(length);
		} else {
			buffer = ByteBuffer.allocate(length);
		}
		return buffer;
	}

	/**
	 * If there is no listener, and this connection is not for single use,
	 * this method exits. When there is a listener, this method assembles
	 * data into messages by invoking convertAndSend whenever there is
	 * data in the input Stream. Method exits when a message is complete
	 * and there is no more data; thus freeing the thread to work on other
	 * sockets.
	 */
	@Override
	public void run() {
		if (logger.isTraceEnabled()) {
			logger.trace(this.getConnectionId() + " Nio message assembler running...");
		}
		boolean moreDataAvailable = true;
		while(moreDataAvailable) {
			try {
				if (this.getListener() == null && !this.isSingleUse()) {
					logger.debug("TcpListener exiting - no listener and not single use");
					return;
				}
				try {
					if (dataAvailable()) {
						Message<?> message = convert();
						if (dataAvailable()) {
							// there is more data in the pipe; run another assembler
							// to assemble the next message, while we send ours
							this.executionControl.incrementAndGet();
							try {
								this.taskExecutor.execute2(this);
							}
							catch (RejectedExecutionException e) {
								this.executionControl.decrementAndGet();
								if (logger.isInfoEnabled()) {
									logger.info(getConnectionId() + " Insufficient threads in the assembler fixed thread pool; consider " +
											"increasing this task executor pool size; data avail: " + this.channelInputStream.available());
								}
							}
						}
						this.executionControl.decrementAndGet();
						if (message != null) {
							sendToChannel(message);
						}
					}
					else {
						this.executionControl.decrementAndGet();
					}
				}
				catch (Exception e) {
					if (logger.isTraceEnabled()) {
						logger.error("Read exception " +
								 this.getConnectionId(), e);
					}
					else if (!this.isNoReadErrorOnClose()) {
						logger.error("Read exception " +
									 this.getConnectionId() + " " +
									 e.getClass().getSimpleName() +
								     ":" + e.getCause() + ":" + e.getMessage());
					}
					else {
						if (logger.isDebugEnabled()) {
							logger.debug("Read exception " +
										 this.getConnectionId() + " " +
										 e.getClass().getSimpleName() +
									     ":" + e.getCause() + ":" + e.getMessage());
						}
					}
					this.closeConnection(true);
					this.sendExceptionToListener(e);
					return;
				}
			}
			finally {
				moreDataAvailable = false;
				// Final check in case new data came in and the
				// timing was such that we were the last assembler and
				// a new one wasn't run
				try {
					if (dataAvailable()) {
						synchronized(this.executionControl) {
							if (this.executionControl.incrementAndGet() <= 1) {
								// only continue if we don't already have another assembler running
								this.executionControl.set(1);
								moreDataAvailable = true;

							}
							else {
								this.executionControl.decrementAndGet();
							}
						}
					}
					if (moreDataAvailable) {
						if (logger.isTraceEnabled()) {
							logger.trace(this.getConnectionId() + " Nio message assembler continuing...");
						}
					}
					else {
						if (logger.isTraceEnabled()) {
							logger.trace(this.getConnectionId() + " Nio message assembler exiting... avail: " + this.channelInputStream.available());
						}
					}
				}
				catch (IOException e) {
					logger.error("Exception when checking for assembler", e);
				}
			}
		}
	}

	private boolean dataAvailable() throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace(getConnectionId() + " checking data avail: " + this.channelInputStream.available() +
					" pending: " + (this.writingToPipe));
		}
		return writingToPipe || this.channelInputStream.available() > 0;
	}

	/**
	 * Blocks until a complete message has been assembled.
	 * Synchronized to avoid concurrency.
	 * @return The Message or null if no data is available.
	 * @throws IOException
	 */
	private synchronized Message<?> convert() throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace(getConnectionId() + " checking data avail (convert): " + this.channelInputStream.available() +
					" pending: " + (this.writingToPipe));
		}
		if (this.channelInputStream.available() <= 0) {
			try {
				if (this.writingLatch.await(60, TimeUnit.SECONDS)) {
					if (this.channelInputStream.available() <= 0) {
						return null;
					}
				}
				else { // should never happen
					throw new IOException("Timed out waiting for IO");
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted waiting for IO");
			}
		}
		Message<?> message = null;
		try {
			message = this.getMapper().toMessage(this);
		}
		catch (Exception e) {
			this.closeConnection(true);
			if (e instanceof SocketTimeoutException && this.isSingleUse()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Closing single use socket after timeout " + this.getConnectionId());
				}
			} else {
				if (!(e instanceof SoftEndOfStreamException)) {
					throw e;
				}
			}
			return null;
		}
		return message;
	}

	private void sendToChannel(Message<?> message) {
		boolean intercepted = false;
		try {
			if (message != null) {
				intercepted = getListener().onMessage(message);
			}
		}
		catch (Exception e) {
			if (e instanceof NoListenerException) {
				if (this.isSingleUse()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Closing single use channel after inbound message " + this.getConnectionId());
					}
					this.closeConnection(true);
				}
			}
			else {
				logger.error("Exception sending message: " + message, e);
			}
		}
		/*
		 * For single use sockets, we close after receipt if we are on the client
		 * side, and the data was not intercepted,
		 * or the server side has no outbound adapter registered
		 */
		if (this.isSingleUse() && ((!this.isServer() && !intercepted) || (this.isServer() && this.getSender() == null))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing single use channel after inbound message " + this.getConnectionId());
			}
			this.closeConnection(false);
		}
	}

	private void doRead() throws Exception {
		if (this.rawBuffer == null) {
			this.rawBuffer = allocate(maxMessageSize);
		}

		this.writingLatch = new CountDownLatch(1);
		this.writingToPipe = true;
		try {
			if (this.taskExecutor == null) {
				ExecutorService executor = Executors.newCachedThreadPool();
				this.taskExecutor = new CompositeExecutor(executor, executor);
			}
			// If there is no assembler running, start one
			checkForAssembler();

			if (logger.isTraceEnabled()) {
				logger.trace("Before read:" + this.rawBuffer.position() + "/" + this.rawBuffer.limit());
			}
			int len = this.socketChannel.read(this.rawBuffer);
			if (len < 0) {
				this.writingToPipe = false;
				this.closeConnection(true);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("After read:" + this.rawBuffer.position() + "/" + this.rawBuffer.limit());
			}
			this.rawBuffer.flip();
			if (logger.isTraceEnabled()) {
				logger.trace("After flip:" + this.rawBuffer.position() + "/" + this.rawBuffer.limit());
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Read " + rawBuffer.limit() + " into raw buffer");
			}
			this.sendToPipe(rawBuffer);
		}
		catch (RejectedExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			this.publishConnectionExceptionEvent(e);
			throw e;
		}
		finally {
			this.writingToPipe = false;
			this.writingLatch.countDown();
		}
	}

	protected void sendToPipe(ByteBuffer rawBuffer) throws IOException {
		Assert.notNull(rawBuffer, "rawBuffer cannot be null");
		if (logger.isTraceEnabled()) {
			logger.trace(this.getConnectionId() + " Sending " + rawBuffer.limit() + " to pipe");
		}
		this.channelInputStream.write(rawBuffer.array(), rawBuffer.limit());
		rawBuffer.clear();
	}

	private void checkForAssembler() {
		synchronized(this.executionControl) {
			if (this.executionControl.incrementAndGet() <= 1) {
				// only execute run() if we don't already have one running
				this.executionControl.set(1);
				if (logger.isDebugEnabled()) {
					logger.debug(this.getConnectionId() + " Running an assembler");
				}
				try {
					this.taskExecutor.execute2(this);
				}
				catch (RejectedExecutionException e) {
					this.executionControl.decrementAndGet();
					if (logger.isInfoEnabled()) {
						logger.info("Insufficient threads in the assembler fixed thread pool; consider increasing " +
								"this task executor pool size");
					}
					throw e;
				}
			}
			else {
				this.executionControl.decrementAndGet();
			}
		}
	}

	/**
	 * Invoked by the factory when there is data to be read.
	 */
	public void readPacket() {
		if (logger.isDebugEnabled()) {
			logger.debug(this.getConnectionId() + " Reading...");
		}
		try {
			doRead();
		}
		catch (ClosedChannelException cce) {
			if (logger.isDebugEnabled()) {
				logger.debug(this.getConnectionId() + " Channel is closed");
			}
			this.closeConnection(true);
		}
		catch (RejectedExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("Exception on Read " +
					     this.getConnectionId() + " " +
					     e.getMessage(), e);
			this.closeConnection(true);
		}
	}

	/**
	 * Close the socket due to timeout.
	 */
	void timeout() {
		this.closeConnection(true);
	}

	/**
	 *
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		if (taskExecutor instanceof CompositeExecutor) {
			this.taskExecutor = (CompositeExecutor) taskExecutor;
		}
		else {
			this.taskExecutor = new CompositeExecutor(taskExecutor, taskExecutor);
		}
	}

	/**
	 * If true, connection will attempt to use direct buffers where
	 * possible.
	 * @param usingDirectBuffers the usingDirectBuffers to set.
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	protected boolean isUsingDirectBuffers() {
		return usingDirectBuffers;
	}

	protected ChannelOutputStream getChannelOutputStream() {
		return channelOutputStream;
	}

	/**
	 *
	 * @return Time of last read.
	 */
	public long getLastRead() {
		return lastRead;
	}

	/**
	 *
	 * @param lastRead The time of the last read.
	 */
	public void setLastRead(long lastRead) {
		this.lastRead = lastRead;
	}

	/**
	 * @return the time of the last send
	 */
	public long getLastSend() {
		return lastSend;
	}

	/**
	 * OutputStream to wrap a SocketChannel; implements timeout on write.
	 *
	 */
	class ChannelOutputStream extends OutputStream {

		private Selector selector;

		private int soTimeout;

		@Override
		public void write(int b) throws IOException {
			byte[] bytes = new byte[1];
			bytes[0] = (byte) b;
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			doWrite(buffer);
		}

		@Override
		public void close() throws IOException {
			doClose();
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(b, off, len);
			doWrite(buffer);
		}

		@Override
		public void write(byte[] b) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(b);
			doWrite(buffer);
		}

		protected synchronized void doWrite(ByteBuffer buffer) throws IOException {
			if (logger.isDebugEnabled()) {
				logger.debug(getConnectionId() + " writing " + buffer.remaining());
			}
			socketChannel.write(buffer);
			int remaining = buffer.remaining();
			if (remaining == 0) {
				return;
			}
			if (this.selector == null) {
				this.selector = Selector.open();
				this.soTimeout = socketChannel.socket().getSoTimeout();
			}
			socketChannel.register(selector, SelectionKey.OP_WRITE);
			while (remaining > 0) {
				int selectionCount = this.selector.select(this.soTimeout);
				if (selectionCount == 0) {
					throw new SocketTimeoutException("Timeout on write");
				}
				selector.selectedKeys().clear();
				socketChannel.write(buffer);
				remaining = buffer.remaining();
			}
		}

	}

	/**
	 * Provides an InputStream to receive data from {@link SocketChannel#read(ByteBuffer)}
	 * operations. Each new buffer is added to a BlockingQueue; when the reading thread
	 * exhausts the current buffer, it retrieves the next from the queue.
	 * Writes block for up to the pipeTimeout if 5 buffers are queued to be read.
	 *
	 */
	class ChannelInputStream extends InputStream {

		private static final int BUFFER_LIMIT = 5;

		private final BlockingQueue<byte[]> buffers = new LinkedBlockingQueue<byte[]>(BUFFER_LIMIT);

		private volatile byte[] currentBuffer;

		private volatile int currentOffset;

		private final AtomicInteger available = new AtomicInteger();

		private volatile boolean isClosed;

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			Assert.notNull(b, "byte[] cannot be null");
			if (off < 0 || len < 0 || len > b.length - off) {
			    throw new IndexOutOfBoundsException();
			}
			else if (len == 0) {
			    return 0;
			}

			int n = 0;
			while ((this.available.get() > 0 || n == 0) &&
						n < len) {
				int bite = read();
				if (bite < 0) {
					if (n == 0) {
						return -1;
					}
					else {
						return n;
					}
				}
				b[off + n++] = (byte) bite;
			}
			return n;
		}

		@Override
		public synchronized int read() throws IOException {
			if (this.isClosed && available.get() == 0) {
				return -1;
			}
			if (this.currentBuffer == null) {
				this.currentBuffer = getNextBuffer();
				this.currentOffset = 0;
				if (this.currentBuffer == null) {
					return -1;
				}
			}
			int bite;
			bite = this.currentBuffer[this.currentOffset++] & 0xff;
			this.available.decrementAndGet();
			if (this.currentOffset >= this.currentBuffer.length) {
				this.currentBuffer = null;
			}
			return bite;
		}

		private byte[] getNextBuffer() throws IOException {
			byte[] buffer = null;
			while (buffer == null) {
				try {
					buffer = buffers.poll(1, TimeUnit.SECONDS);
					if (buffer == null && this.isClosed) {
						return null;
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while waiting for data", e);
				}
			}
			return buffer;
		}

		/**
		 * Blocks if the blocking queue already contains 5 buffers.
		 * @param array
		 * @param bytesToWrite
		 * @throws IOException
		 */
		public void write(byte[] array, int bytesToWrite) throws IOException {
			if (bytesToWrite > 0) {
				byte[] buffer = new byte[bytesToWrite];
				System.arraycopy(array, 0, buffer, 0, bytesToWrite);
				this.available.addAndGet(bytesToWrite);
				if (TcpNioConnection.this.writingLatch != null) {
					TcpNioConnection.this.writingLatch.countDown();
				}
				try {
					if (!this.buffers.offer(buffer, pipeTimeout, TimeUnit.MILLISECONDS)) {
						throw new IOException("Timed out waiting for buffer space");
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while waiting for buffer space", e);
				}
				TcpNioConnection.this.writingLatch = new CountDownLatch(1);
			}
		}

		@Override
		public void close() throws IOException {
			super.close();
			this.isClosed = true;
		}

		@Override
		public int available() throws IOException {
			return this.available.get();
		}

	}
}
