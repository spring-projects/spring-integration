/*
 * Copyright 2016-2023 the original author or authors.
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

import java.util.concurrent.Executor;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpSocketSupport;

/**
 * An {@link IntegrationComponentSpec} for {@link AbstractConnectionFactory}s.
 *
 * @param <S> the target {@link AbstractConnectionFactorySpec} implementation type.
 * @param <C> the target {@link AbstractConnectionFactory} implementation type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public abstract class AbstractConnectionFactorySpec
		<S extends AbstractConnectionFactorySpec<S, C>, C extends AbstractConnectionFactory>
		extends IntegrationComponentSpec<S, C> {

	protected AbstractConnectionFactorySpec(C connectionFactory) {
		this.target = connectionFactory;
	}

	@Override
	public S id(String id) {
		this.target.setBeanName(id);
		return _this();
	}

	/**
	 * @param soTimeout the timeout socket option, in milliseconds.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSoTimeout(int)
	 */
	public S soTimeout(int soTimeout) {
		this.target.setSoTimeout(soTimeout);
		return _this();
	}

	/**
	 * @param soReceiveBufferSize the receive buffer size socket option.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSoReceiveBufferSize(int)
	 */
	public S soReceiveBufferSize(int soReceiveBufferSize) {
		this.target.setSoReceiveBufferSize(soReceiveBufferSize);
		return _this();
	}

	/**
	 * @param soSendBufferSize the send buffer size socket option.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSoSendBufferSize(int)
	 */
	public S soSendBufferSize(int soSendBufferSize) {
		this.target.setSoSendBufferSize(soSendBufferSize);
		return _this();
	}

	/**
	 * @param soTcpNoDelay the TCP no delay socket option (disable Nagle's algorithm).
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSoTcpNoDelay(boolean)
	 */
	public S soTcpNoDelay(boolean soTcpNoDelay) {
		this.target.setSoTcpNoDelay(soTcpNoDelay);
		return _this();
	}

	/**
	 * @param soLinger the linger socket option.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSoLinger(int)
	 */
	public S soLinger(int soLinger) {
		this.target.setSoLinger(soLinger);
		return _this();
	}

	/**
	 * @param soKeepAlive the keep alive socket option.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSoKeepAlive(boolean)
	 */
	public S soKeepAlive(boolean soKeepAlive) {
		this.target.setSoKeepAlive(soKeepAlive);
		return _this();
	}

	/**
	 * @param soTrafficClass the traffic class socket option.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSoTrafficClass(int)
	 */
	public S soTrafficClass(int soTrafficClass) {
		this.target.setSoTrafficClass(soTrafficClass);
		return _this();
	}

	/**
	 * @param taskExecutor the task executor.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setTaskExecutor(Executor)
	 */
	public S taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return _this();
	}

	/**
	 * @param deserializer the deserializer.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setDeserializer(Deserializer)
	 */
	public S deserializer(Deserializer<?> deserializer) {
		this.target.setDeserializer(deserializer);
		return _this();
	}

	/**
	 * @param serializer the serializer.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setSerializer(Serializer)
	 */
	public S serializer(Serializer<?> serializer) {
		this.target.setSerializer(serializer);
		return _this();
	}

	/**
	 * @param mapper the message mapper.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setMapper(TcpMessageMapper)
	 */
	public S mapper(TcpMessageMapper mapper) {
		this.target.setMapper(mapper);
		return _this();
	}

	/**
	 * @param leaveOpen true to leave the socket open for additional messages.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setLeaveOpen(boolean)
	 */
	public S leaveOpen(boolean leaveOpen) {
		this.target.setLeaveOpen(leaveOpen);
		return _this();
	}

	/**
	 * @param interceptorFactoryChain the interceptor factory chain.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setInterceptorFactoryChain(TcpConnectionInterceptorFactoryChain)
	 */
	public S interceptorFactoryChain(TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		this.target.setInterceptorFactoryChain(interceptorFactoryChain);
		return _this();
	}

	/**
	 * @param lookupHost true to reverse lookup the host.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setLookupHost(boolean)
	 */
	public S lookupHost(boolean lookupHost) {
		this.target.setLookupHost(lookupHost);
		return _this();
	}

	/**
	 * @param nioHarvestInterval the harvest interval when using NIO.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setNioHarvestInterval(int)
	 */
	public S nioHarvestInterval(int nioHarvestInterval) {
		this.target.setNioHarvestInterval(nioHarvestInterval);
		return _this();
	}

	/**
	 * @param readDelay the read delay.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setReadDelay(long)
	 */
	public S readDelay(long readDelay) {
		this.target.setReadDelay(readDelay);
		return _this();
	}

	/**
	 * @param tcpSocketSupport the {@link TcpSocketSupport}.
	 * @return the spec.
	 * @see AbstractConnectionFactory#setTcpSocketSupport(TcpSocketSupport)
	 */
	public S socketSupport(TcpSocketSupport tcpSocketSupport) {
		this.target.setTcpSocketSupport(tcpSocketSupport);
		return _this();
	}

	/**
	 * This connection factory uses a new connection for each operation.
	 * @param single true for a new connection for each operation.
	 * @return the spec.
	 * @since 5.2
	 */
	public S singleUseConnections(boolean single) {
		this.target.setSingleUse(single);
		return _this();
	}

}
