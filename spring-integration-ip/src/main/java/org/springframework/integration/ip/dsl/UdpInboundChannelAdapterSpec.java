/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import java.net.DatagramSocket;
import java.util.concurrent.Executor;

import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.ip.udp.MulticastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link MessageProducerSpec} for {@link UnicastReceivingChannelAdapter}s.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class UdpInboundChannelAdapterSpec<S extends UdpInboundChannelAdapterSpec<S>>
	extends MessageProducerSpec<S, UnicastReceivingChannelAdapter> {

	UdpInboundChannelAdapterSpec(int port) {
		super(new UnicastReceivingChannelAdapter(port));
	}

	UdpInboundChannelAdapterSpec(int port, String multicastGroup) {
		super(new MulticastReceivingChannelAdapter(multicastGroup, port));
	}

	/**
	 * @param soTimeout set the timeout socket option.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSoTimeout(int)
	 */
	public S soTimeout(int soTimeout) {
		this.target.setSoTimeout(soTimeout);
		return _this();
	}

	/**
	 * @param taskScheduler set the task scheduler.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setTaskScheduler(TaskScheduler)
	 */
	public S taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return _this();
	}

	/**
	 * @param soReceiveBufferSize set the receive buffer size socket option.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSoReceiveBufferSize(int)
	 */
	public S soReceiveBufferSize(int soReceiveBufferSize) {
		this.target.setSoReceiveBufferSize(soReceiveBufferSize);
		return _this();
	}

	/**
	 * @param receiveBufferSize set the receive buffer size.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setReceiveBufferSize(int)
	 */
	public S receiveBufferSize(int receiveBufferSize) {
		this.target.setReceiveBufferSize(receiveBufferSize);
		return _this();
	}

	/**
	 * @param lengthCheck set the length check boolean.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setLengthCheck(boolean)
	 */
	public S lengthCheck(boolean lengthCheck) {
		this.target.setLengthCheck(lengthCheck);
		return _this();
	}

	/**
	 * @param localAddress set the local address.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setLocalAddress(String)
	 */
	public S localAddress(String localAddress) {
		this.target.setLocalAddress(localAddress);
		return _this();
	}

	/**
	 * @param poolSize set the pool size.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setPoolSize(int)
	 */
	public S poolSize(int poolSize) {
		this.target.setPoolSize(poolSize);
		return _this();
	}

	/**
	 * @param taskExecutor set the task executor.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setTaskExecutor(Executor)
	 */
	public S TaskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return _this();
	}

	/**
	 * @param socket set the socket.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSocket(DatagramSocket)
	 */
	public S socket(DatagramSocket socket) {
		this.target.setSocket(socket);
		return _this();
	}

	/**
	 * @param soSendBufferSize set the send bufffer size socket option.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setSoSendBufferSize(int)
	 */
	public S soSendBufferSize(int soSendBufferSize) {
		this.target.setSoSendBufferSize(soSendBufferSize);
		return _this();
	}

	/**
	 * @param lookupHost set true to reverse lookup the host.
	 * @return the spec.
	 * @see UnicastReceivingChannelAdapter#setLookupHost(boolean)
	 */
	public S LookupHost(boolean lookupHost) {
		this.target.setLookupHost(lookupHost);
		return _this();
	}

}
