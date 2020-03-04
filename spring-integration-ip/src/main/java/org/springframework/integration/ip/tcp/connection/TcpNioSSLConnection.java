/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Implementation of {@link TcpConnection} supporting SSL/TLS over NIO.
 * Unlike TcpNetConnection, which uses Sockets, the JVM does not directly support SSL for
 * SocketChannels, used by NIO. Instead, the SSLEngine is provided whereby the SSL
 * encryption is performed by passing in a plain text buffer, and receiving an
 * encrypted buffer to transmit over the network. Similarly, encrypted data read from
 * the network is decrypted.<p>
 * However, before this can be done, certain handshaking operations are required, involving
 * the creation of data buffers which must be exchanged by the peers. A number of such
 * transfers are required; once the handshake is finished, it is relatively simple to
 * encrypt/decrypt the data.<p>
 * Also, it may be deemed necessary to re-perform handshaking.<p>
 * This class supports the management of handshaking as necessary, both from the
 * initiating and receiving peers.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class TcpNioSSLConnection extends TcpNioConnection {

	private static final int DEFAULT_HANDSHAKE_TIMEOUT = 30;

	private final SSLEngine sslEngine;

	private ByteBuffer decoded;

	private ByteBuffer encoded;

	private final Semaphore semaphore = new Semaphore(0);

	private final Object monitorLock = new Object();

	private int handshakeTimeout = DEFAULT_HANDSHAKE_TIMEOUT;

	private boolean needMoreNetworkData;

	private SSLHandshakeException sslFatal;

	private volatile SSLChannelOutputStream sslChannelOutputStream;

	private volatile boolean writerActive;

	public TcpNioSSLConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
			@Nullable ApplicationEventPublisher applicationEventPublisher, @Nullable String connectionFactoryName,
			SSLEngine sslEngine) {

		super(socketChannel, server, lookupHost, applicationEventPublisher, connectionFactoryName);
		this.sslEngine = sslEngine;
	}

	/**
	 * Set the timeout while waiting for handshake data (in seconds). Default 30.
	 * @param handshakeTimeout the timeout.
	 * @since 4.3.6
	 */
	public void setHandshakeTimeout(int handshakeTimeout) {
		this.handshakeTimeout = handshakeTimeout;
	}

	@Override
	public SSLSession getSslSession() {
		return this.sslEngine.getSession();
	}

	/**
	 * Overrides super class method to perform decryption and/or participate
	 * in handshaking. Decrypted data is sent to the super class to be
	 * assembled into a Message. Data received from the network may
	 * constitute multiple SSL packets, and may end with a partial
	 * packet. In that case, the buffer is compacted, ready to receive
	 * the remainder of the packet.
	 */
	@Override
	protected void sendToPipe(final ByteBuffer networkBuffer) throws IOException {
		Assert.notNull(networkBuffer, "rawBuffer cannot be null");
		if (logger.isDebugEnabled()) {
			logger.debug("sendToPipe " + this.sslEngine.getHandshakeStatus() + ", remaining: " +
					networkBuffer.remaining());
		}
		SSLEngineResult result = null;
		while (!this.needMoreNetworkData) {
			try {
				result = decode(networkBuffer);
			}
			catch (SSLHandshakeException e) {
				this.sslFatal = e;
				this.semaphore.release();
				throw e;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("result " + resultToString(result) + ", remaining: " + networkBuffer.remaining());
			}
		}
		this.needMoreNetworkData = false;
		if (Status.BUFFER_UNDERFLOW == result.getStatus()) {
			networkBuffer.compact();
		}
		else {
			networkBuffer.clear();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("sendToPipe.x " + resultToString(result) + ", remaining: " + networkBuffer.remaining());
		}
	}

	/**
	 * Performs the actual decryption of a received packet - which may be real
	 * data, or handshaking data. Appropriate action is taken with the data.
	 * If this side did not initiate the handshake, any handshaking data sent out
	 * is handled by the thread running in the {@link SSLChannelOutputStream#doWrite(ByteBuffer)}
	 * method, which is awoken here, as a result of reaching that stage in the handshaking.
	 */
	@SuppressWarnings("fallthrough")
	private SSLEngineResult decode(ByteBuffer networkBuffer) throws IOException {
		SSLEngineResult result = new SSLEngineResult(Status.OK, this.sslEngine.getHandshakeStatus(), 0, 0);
		HandshakeStatus handshakeStatus = this.sslEngine.getHandshakeStatus();
		switch (handshakeStatus) {
			case NEED_TASK:
				runTasks();
				break;
			case NEED_UNWRAP:
			case FINISHED:
			case NOT_HANDSHAKING:
				this.decoded.clear();
				result = this.sslEngine.unwrap(networkBuffer, this.decoded);
				if (logger.isDebugEnabled()) {
					logger.debug("After unwrap: " + resultToString(result));
				}
				Status status = result.getStatus();
				if (status == Status.BUFFER_OVERFLOW) {
					this.decoded =
							this.allocateEncryptionBuffer(this.sslEngine.getSession().getApplicationBufferSize());
				}
				if (result.bytesProduced() > 0) {
					this.decoded.flip();
					super.sendToPipe(this.decoded);
				}
				break;
			case NEED_WRAP:
				if (!resumeWriterIfNeeded()) {
					this.encoded.clear();
					result = this.sslEngine.wrap(networkBuffer, this.encoded);
					if (logger.isDebugEnabled()) {
						logger.debug("After wrap: " + resultToString(result));
					}
					if (result.getStatus() == Status.BUFFER_OVERFLOW) {
						this.encoded = this.allocateEncryptionBuffer(this.sslEngine.getSession().getPacketBufferSize());
					}
					else {
						this.encoded.flip();
						getSSLChannelOutputStream().writeEncoded(this.encoded);
					}
				}
				break;
			default:
		}
		switch (result.getHandshakeStatus()) {
			case FINISHED:
				resumeWriterIfNeeded(); //NOSONAR - fall-through intended
				// switch fall-through intended
			case NOT_HANDSHAKING:
			case NEED_UNWRAP:
				this.needMoreNetworkData = result.getStatus() == Status.BUFFER_UNDERFLOW || networkBuffer
						.remaining() == 0;
				break;
			default:
		}
		return result;
	}

	/**
	 * Handshake sends are handled by the initiator.
	 * @return false if we are the initiator.
	 */
	private boolean resumeWriterIfNeeded() {
		if (this.writerActive) {
			if (logger.isTraceEnabled()) {
				logger.trace("Waking sender, permits: " + this.semaphore.availablePermits());
			}
			this.semaphore.release();
			return true;
		}
		return false;
	}

	/**
	 * Part of the SSLEngine handshaking protocol required at
	 * various stages. Tasks are run on the current thread.
	 */
	private void runTasks() {
		Runnable task;
		while ((task = this.sslEngine.getDelegatedTask()) != null) {
			task.run();
		}
	}

	/**
	 * Determines whether {@link #runTasks()} is needed and invokes if so.
	 */
	private HandshakeStatus runTasksIfNeeded(SSLEngineResult result) {
		if (logger.isDebugEnabled()) {
			logger.debug("Running tasks if needed " + resultToString(result));
		}
		if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			runTasks();
		}
		HandshakeStatus handshakeStatus = this.sslEngine.getHandshakeStatus();
		if (logger.isDebugEnabled()) {
			logger.debug("New handshake status " + handshakeStatus);
		}
		return handshakeStatus;
	}

	/**
	 * Initializes the SSLEngine and sets up the encryption/decryption buffers.
	 */
	public void init() {
		if (this.decoded == null) {
			this.decoded = allocateEncryptionBuffer(2048);
			this.encoded = allocateEncryptionBuffer(2048);
			initializeEngine();
		}
	}

	private ByteBuffer allocateEncryptionBuffer(int size) {
		if (isUsingDirectBuffers()) {
			return ByteBuffer.allocateDirect(size);
		}
		else {
			return ByteBuffer.allocate(size);
		}
	}

	private void initializeEngine() {
		boolean client = !isServer();
		this.sslEngine.setUseClientMode(client);
	}

	@Override
	protected ChannelOutputStream getChannelOutputStream() {
		synchronized (this.monitorLock) {
			if (this.sslChannelOutputStream == null) {
				this.sslChannelOutputStream = new SSLChannelOutputStream(super.getChannelOutputStream());
			}
			return this.sslChannelOutputStream;
		}
	}

	protected SSLChannelOutputStream getSSLChannelOutputStream() {
		if (this.sslChannelOutputStream == null) {
			return (SSLChannelOutputStream) getChannelOutputStream();
		}
		else {
			return this.sslChannelOutputStream;
		}
	}

	private String resultToString(SSLEngineResult result) {
		return result.toString().replace('\n', ' ');
	}

	@Override
	public void close() {
		super.close();
		this.semaphore.release();
	}

	/**
	 * Subclass of {@link TcpNioConnection.ChannelOutputStream} to handle encryption
	 * of outbound data. Wraps an instance of the superclass, which is invoked to
	 * send to encrypted data to the SocketChannel.
	 *
	 */
	final class SSLChannelOutputStream extends ChannelOutputStream {

		private final ChannelOutputStream channelOutputStream;

		SSLChannelOutputStream(ChannelOutputStream channelOutputStream) {
			this.channelOutputStream = channelOutputStream;
		}

		/**
		 * Encrypts the plaintText buffer and writes it to the SocketChannel.
		 * Will participate in SSL handshaking as necessary. For very large
		 * data, the SSL packets will be limited by the engine's buffer sizes
		 * and multiple writes will be necessary.
		 */
		@Override
		protected synchronized void doWrite(ByteBuffer plainText)
				throws IOException {
			try {
				TcpNioSSLConnection.this.writerActive = true;
				int remaining = plainText.remaining();
				while (remaining > 0) {
					SSLEngineResult result = encode(plainText);
					if (logger.isDebugEnabled()) {
						logger.debug("doWrite: " + resultToString(result));
					}
					if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
						writeEncodedIfAny();
						if (plainText.remaining() >= remaining) {
							throw new MessagingException(
									"Unexpected condition - SSL wrap did not consume any data; remaining = "
											+ remaining);
						}
						remaining = plainText.remaining();
					}
					else {
						doClientSideHandshake(plainText, result);
						writeEncodedIfAny();
					}
				}
			}
			finally {
				TcpNioSSLConnection.this.writerActive = false;
			}
		}

		/**
		 * Handles SSL handshaking; when network data is needed from the peer, suspends
		 * until that data is received.
		 */
		private void doClientSideHandshake(ByteBuffer plainText, SSLEngineResult resultArg) throws IOException {
			SSLEngineResult result = resultArg;
			TcpNioSSLConnection.this.semaphore.drainPermits();
			HandshakeStatus status = TcpNioSSLConnection.this.sslEngine.getHandshakeStatus();
			while (status != HandshakeStatus.FINISHED) {
				writeEncodedIfAny();
				status = runTasksIfNeeded(result);
				if (status == HandshakeStatus.NEED_UNWRAP) {
					status = waitForHandshakeData(result);
				}
				if (status == HandshakeStatus.NEED_WRAP ||
						status == HandshakeStatus.NOT_HANDSHAKING ||
						status == HandshakeStatus.FINISHED) {
					result = encode(plainText);
					status = result.getHandshakeStatus();
					if (status == HandshakeStatus.NOT_HANDSHAKING ||
							status == HandshakeStatus.FINISHED) {
						break;
					}
				}
				else {
					logger.debug(status);
				}
			}
		}

		private void writeEncodedIfAny() throws IOException {
			TcpNioSSLConnection.this.encoded.flip();
			writeEncoded(TcpNioSSLConnection.this.encoded);
			TcpNioSSLConnection.this.encoded.clear();
		}

		/**
		 * Suspend processing until data is received from the peer.
		 */
		private HandshakeStatus waitForHandshakeData(SSLEngineResult result) throws IOException {

			try {
				logger.trace("Writer waiting for handshake");
				if (!TcpNioSSLConnection.this.semaphore.tryAcquire(TcpNioSSLConnection.this.handshakeTimeout,
						TimeUnit.SECONDS)) {
					throw new MessagingException("SSL Handshaking taking too long");
				}
				else {
					if (TcpNioSSLConnection.this.sslFatal != null) {
						throw TcpNioSSLConnection.this.sslFatal;
					}
					else if (!isOpen()) {
						throw new IOException("Socket closed during SSL Handshake");
					}
				}
				logger.trace("Writer resuming handshake");
				return runTasksIfNeeded(result);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new MessagingException("Interrupted during SSL Handshaking", e);
			}
		}

		/**
		 * Encrypts plain text data. The result may indicate handshaking is needed.
		 */
		private SSLEngineResult encode(ByteBuffer plainText) throws IOException {
			TcpNioSSLConnection.this.encoded.clear();
			SSLEngineResult result =
					TcpNioSSLConnection.this.sslEngine.wrap(plainText, TcpNioSSLConnection.this.encoded);
			if (logger.isDebugEnabled()) {
				logger.debug("After wrap: "
						+ resultToString(result)
						+ " Plaintext buffer @" + plainText.position() + "/" + plainText.limit());
			}
			if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
				TcpNioSSLConnection.this.encoded =
						allocateEncryptionBuffer(TcpNioSSLConnection.this.sslEngine.getSession().getPacketBufferSize());
				result = TcpNioSSLConnection.this.sslEngine.wrap(plainText, TcpNioSSLConnection.this.encoded);
			}
			return result;
		}

		/**
		 * Write data to the SocketChannel.
		 */
		void writeEncoded(ByteBuffer encoded) throws IOException {
			this.channelOutputStream.doWrite(encoded);
		}

	}

}
