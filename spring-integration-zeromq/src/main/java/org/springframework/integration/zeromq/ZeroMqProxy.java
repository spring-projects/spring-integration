/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.zeromq;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * This class encapsulates a logic to configure and manage ZeroMQ proxy.
 * It binds frontend and backend sockets over TCP on all the available network interfaces
 * with provided ports or randomly selected otherwise.
 * <p>
 * The {@link ZeroMqProxy.Type} dictates which pair of ZeroMQ sockets to bind with this proxy
 * to implement any possible patterns for ZeroMQ intermediary. Defaults to @link {@link ZeroMqProxy.Type#PULL_PUSH}.
 * <p>
 * The control socket is exposed as a {@link SocketType#PAIR} with an inter-thread transport
 * on tne {@code "inproc://" + beanName + ".control"} address - can be obtained via {@link #getControlAddress()}.
 * Should be used with the same application from {@link SocketType#PAIR} socket to send
 * {@link zmq.ZMQ#PROXY_TERMINATE}, {@link zmq.ZMQ#PROXY_PAUSE} and/or {@link zmq.ZMQ#PROXY_RESUME} commands.
 * <p>
 * If proxy cannot be started for some reason, the error message is logged respectively and this component is
 * left in the non-starting state.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 *
 * @see ZMQ#proxy(ZMQ.Socket, ZMQ.Socket, ZMQ.Socket)
 */
public class ZeroMqProxy implements InitializingBean, SmartLifecycle, BeanNameAware {

	private static final Log LOG = LogFactory.getLog(ZeroMqProxy.class);

	private final ZContext context;

	private final Type type;

	private final AtomicBoolean running = new AtomicBoolean();

	private final AtomicInteger frontendPort = new AtomicInteger();

	private final AtomicInteger backendPort = new AtomicInteger();

	private String controlAddress;

	private Executor proxyExecutor;

	@Nullable
	private Consumer<ZMQ.Socket> frontendSocketConfigurer;

	@Nullable
	private Consumer<ZMQ.Socket> backendSocketConfigurer;

	private String beanName;

	private boolean autoStartup;

	private int phase;

	public ZeroMqProxy(ZContext context) {
		this(context, Type.PULL_PUSH);
	}

	public ZeroMqProxy(ZContext context, Type type) {
		Assert.notNull(context, "'context' must not be null");
		Assert.notNull(type, "'type' must not be null");
		this.context = context;
		this.type = type;
	}

	public void setProxyExecutor(Executor proxyExecutor) {
		Assert.notNull(proxyExecutor, "'proxyExecutor' must not be null");
		this.proxyExecutor = proxyExecutor;
	}

	public void setFrontendPort(int frontendPort) {
		Assert.isTrue(frontendPort > 0, "'frontendPort' must not be zero or negative");
		this.frontendPort.set(frontendPort);
	}

	public void setBackendPort(int backendPort) {
		Assert.isTrue(backendPort > 0, "'backendPort' must not be zero or negative");
		this.backendPort.set(backendPort);
	}

	/**
	 * Provide a {@link Consumer} to configure a proxy frontend socket with arbitrary options, like security.
	 * @param frontendSocketConfigurer the configurer for frontend socket
	 */
	public void setFrontendSocketConfigurer(@Nullable Consumer<ZMQ.Socket> frontendSocketConfigurer) {
		this.frontendSocketConfigurer = frontendSocketConfigurer;
	}

	/**
	 * Provide a {@link Consumer} to configure a proxy backend socket with arbitrary options, like security.
	 * @param backendSocketConfigurer the configurer for backend socket
	 */
	public void setBackendSocketConfigurer(@Nullable Consumer<ZMQ.Socket> backendSocketConfigurer) {
		this.backendSocketConfigurer = backendSocketConfigurer;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public Type getType() {
		return this.type;
	}

	/**
	 * Return the port a frontend socket is bound or 0 if this proxy has not been started yet.
	 * @return the port for a frontend socket or 0
	 */
	public int getFrontendPort() {
		return this.frontendPort.get();
	}

	/**
	 * Return the port a backend socket is bound or null if this proxy has not been started yet.
	 * @return the port for a backend socket or 0
	 */
	public int getBackendPort() {
		return this.backendPort.get();
	}

	/**
	 * Return the address an {@code inproc} control socket is bound if this proxy has not been started yet.
	 * @return the the address for control socket or null
	 */
	@Nullable
	public String getControlAddress() {
		return this.controlAddress;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.proxyExecutor == null) {
			proxyExecutor = Executors.newSingleThreadExecutor(new CustomizableThreadFactory(this.beanName));
		}
		this.controlAddress = "inproc://" + this.beanName + ".control";
	}

	@Override
	public synchronized void start() {
		if (!this.running.get()) {
			this.proxyExecutor
					.execute(() -> {
						try (ZMQ.Socket frontendSocket = this.context.createSocket(this.type.getFrontendSocketType());
							 ZMQ.Socket backendSocket = this.context.createSocket(this.type.getBackendSocketType());
							 ZMQ.Socket controlSocket = this.context.createSocket(SocketType.PAIR)) {

							if (this.frontendSocketConfigurer != null) {
								this.frontendSocketConfigurer.accept(frontendSocket);
							}

							if (this.backendSocketConfigurer != null) {
								this.backendSocketConfigurer.accept(backendSocket);
							}

							this.frontendPort.set(bindSocket(frontendSocket, this.frontendPort.get()));
							this.backendPort.set(bindSocket(backendSocket, this.backendPort.get()));
							boolean bound = controlSocket.bind(this.controlAddress);
							if (!bound) {
								throw new IllegalArgumentException("Cannot bind ZeroMQ socket to address: "
										+ this.controlAddress);
							}
							this.running.set(true);
							ZMQ.proxy(frontendSocket, backendSocket, null, controlSocket);
						}
						catch (Exception ex) {
							LOG.error("Cannot start ZeroMQ proxy from bean: " + this.beanName, ex);
						}
					});
		}
	}

	@Override
	public synchronized void stop() {
		if (this.running.getAndSet(false)) {
			try (ZMQ.Socket commandSocket = context.createSocket(SocketType.PAIR)) {
				commandSocket.connect(this.controlAddress);
				commandSocket.send(zmq.ZMQ.PROXY_TERMINATE);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	private static int bindSocket(ZMQ.Socket socket, int port) {
		if (port == 0) {
			return socket.bindToRandomPort("tcp://*");
		}
		else {
			boolean bound = socket.bind("tcp://*:" + port);
			if (!bound) {
				throw new IllegalArgumentException("Cannot bind ZeroMQ socket to port: " + port);
			}
			return port;
		}
	}

	public enum Type {

		SUB_PUB(SocketType.XSUB, SocketType.XPUB),

		PULL_PUSH(SocketType.PULL, SocketType.PUSH),

		ROUTER_DEALER(SocketType.ROUTER, SocketType.DEALER);

		private final SocketType frontendSocketType;

		private final SocketType backendSocketType;

		Type(SocketType frontendSocketType, SocketType backendSocketType) {
			this.frontendSocketType = frontendSocketType;
			this.backendSocketType = backendSocketType;
		}

		public SocketType getFrontendSocketType() {
			return this.frontendSocketType;
		}

		public SocketType getBackendSocketType() {
			return this.backendSocketType;
		}

	}

}
