/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import java.net.DatagramSocket;
import java.util.concurrent.Executor;

import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.ip.udp.MulticastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.SocketCustomizer;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link MessageProducerSpec} for {@link UnicastReceivingChannelAdapter}s.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public class UdpInboundChannelAdapterSpec
		extends MessageProducerSpec<UdpInboundChannelAdapterSpec, UnicastReceivingChannelAdapter> {

	protected UdpInboundChannelAdapterSpec(int port) {
		super(new UnicastReceivingChannelAdapter(port));
	}

	protected UdpInboundChannelAdapterSpec(int port, String multicastGroup) {
		super(new MulticastReceivingChannelAdapter(multicastGroup, port));
	}

	/**
	 * @param soTimeout set the timeout socket option.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSoTimeout(int)
	 */
	public UdpInboundChannelAdapterSpec soTimeout(int soTimeout) {
		this.target.setSoTimeout(soTimeout);
		return this;
	}

	/**
	 * @param taskScheduler set the task scheduler.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setTaskScheduler(TaskScheduler)
	 */
	public UdpInboundChannelAdapterSpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return this;
	}

	/**
	 * @param soReceiveBufferSize set the receive buffer size socket option.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSoReceiveBufferSize(int)
	 */
	public UdpInboundChannelAdapterSpec soReceiveBufferSize(int soReceiveBufferSize) {
		this.target.setSoReceiveBufferSize(soReceiveBufferSize);
		return this;
	}

	/**
	 * @param receiveBufferSize set the receive buffer size.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setReceiveBufferSize(int)
	 */
	public UdpInboundChannelAdapterSpec receiveBufferSize(int receiveBufferSize) {
		this.target.setReceiveBufferSize(receiveBufferSize);
		return this;
	}

	/**
	 * @param lengthCheck set the length check boolean.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setLengthCheck(boolean)
	 */
	public UdpInboundChannelAdapterSpec lengthCheck(boolean lengthCheck) {
		this.target.setLengthCheck(lengthCheck);
		return this;
	}

	/**
	 * @param localAddress set the local address.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setLocalAddress(String)
	 */
	public UdpInboundChannelAdapterSpec localAddress(String localAddress) {
		this.target.setLocalAddress(localAddress);
		return this;
	}

	/**
	 * @param poolSize set the pool size.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setPoolSize(int)
	 */
	public UdpInboundChannelAdapterSpec poolSize(int poolSize) {
		this.target.setPoolSize(poolSize);
		return this;
	}

	/**
	 * @param taskExecutor set the task executor.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setTaskExecutor(Executor)
	 */
	public UdpInboundChannelAdapterSpec taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

	/**
	 * @param socket set the socket.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSocket(DatagramSocket)
	 */
	public UdpInboundChannelAdapterSpec socket(DatagramSocket socket) {
		this.target.setSocket(socket);
		return this;
	}

	/**
	 * @param soSendBufferSize set the send buffer size socket option.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSoSendBufferSize(int)
	 */
	public UdpInboundChannelAdapterSpec soSendBufferSize(int soSendBufferSize) {
		this.target.setSoSendBufferSize(soSendBufferSize);
		return this;
	}

	/**
	 * @param lookupHost set true to reverse lookup the host.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setLookupHost(boolean)
	 */
	public UdpInboundChannelAdapterSpec lookupHost(boolean lookupHost) {
		this.target.setLookupHost(lookupHost);
		return this;
	}

	/**
	 * Configure the socket.
	 * @param customizer the customizer.
	 * @return the spec.
	 * @since 5.3.3
	 */
	public UdpInboundChannelAdapterSpec configureSocket(SocketCustomizer customizer) {
		this.target.setSocketCustomizer(customizer);
		return this;
	}

}
