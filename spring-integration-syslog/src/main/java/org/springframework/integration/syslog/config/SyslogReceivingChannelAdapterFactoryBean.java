/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.syslog.config;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.syslog.MessageConverter;
import org.springframework.integration.syslog.inbound.SyslogReceivingChannelAdapterSupport;
import org.springframework.integration.syslog.inbound.TcpSyslogReceivingChannelAdapter;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Factory bean to create syslog inbound adapters (UDP or TCP).
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SyslogReceivingChannelAdapterFactoryBean extends AbstractFactoryBean<SyslogReceivingChannelAdapterSupport>
		 implements SmartLifecycle, BeanNameAware, ApplicationEventPublisherAware {

	public enum Protocol { udp, tcp };

	private volatile SyslogReceivingChannelAdapterSupport adapter;

	private final Protocol protocol;

	private volatile MessageChannel outputChannel;

	private volatile boolean autoStartup = true;

	private volatile MessageChannel errorChannel;

	private volatile int phase;

	private volatile Long sendTimeout;

	private volatile AbstractServerConnectionFactory connectionFactory;

	private volatile UnicastReceivingChannelAdapter udpAdapter;

	private volatile Integer port;

	private volatile MessageConverter converter;

	private volatile String beanName;

	private volatile ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Instantiates a factory bean that creates a {@link UdpSyslogReceivingChannelAdapter}
	 * if the protocol is {@link Protocol#udp} or a {@link TcpSyslogReceivingChannelAdapter} if
	 * the protocol is {@link Protocol#tcp}.
	 * @param protocol The protocol.
	 */
	public SyslogReceivingChannelAdapterFactoryBean(Protocol protocol) {
		Assert.notNull(protocol, "'protocol' cannot be null");
		this.protocol = protocol;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setConnectionFactory(AbstractServerConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setUdpAdapter(UnicastReceivingChannelAdapter udpAdapter) {
		this.udpAdapter = udpAdapter;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setConverter(MessageConverter converter) {
		this.converter = converter;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void start() {
		if (this.adapter != null) {
			this.adapter.start();
		}
	}

	@Override
	public void stop() {
		if (this.adapter != null) {
			this.adapter.stop();
		}
	}

	@Override
	public boolean isRunning() {
		if (this.adapter != null) {
			return this.adapter.isRunning();
		}
		return false;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		if (this.adapter != null) {
			this.adapter.stop(callback);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return this.adapter == null ? SyslogReceivingChannelAdapterSupport.class :
			this.adapter.getClass();
	}

	@Override
	protected SyslogReceivingChannelAdapterSupport createInstance() throws Exception {
		SyslogReceivingChannelAdapterSupport adapter;
		if (this.protocol == Protocol.tcp) {
			adapter = new TcpSyslogReceivingChannelAdapter();
			if (this.connectionFactory != null) {
				Assert.isNull(this.port, "Cannot specify both 'port' and 'connectionFactory'");
				((TcpSyslogReceivingChannelAdapter) adapter).setConnectionFactory(this.connectionFactory);
			}
			else if (this.applicationEventPublisher != null) {
				((TcpSyslogReceivingChannelAdapter) adapter).setApplicationEventPublisher(this.applicationEventPublisher);
			}
			Assert.isNull(this.udpAdapter, "Cannot specifiy 'udp-attributes' when the protocol is 'tcp'");
		}
		else if(this.protocol == Protocol.udp) {
			adapter = new UdpSyslogReceivingChannelAdapter();
			if (this.udpAdapter != null) {
				Assert.isNull(this.port, "Cannot specify both 'port' and 'udpAdapter'");
				((UdpSyslogReceivingChannelAdapter) adapter).setUdpAdapter(this.udpAdapter);
			}
			Assert.isNull(this.connectionFactory, "Cannot specifiy 'connection-factory' unless the protocol is 'tcp'");
		}
		else {
			throw new IllegalStateException("Unsupported protocol: " + this.protocol.toString());
		}
		if (this.port != null) {
			adapter.setPort(this.port);
		}
		if (this.outputChannel != null) {
			adapter.setOutputChannel(this.outputChannel);
		}
		adapter.setAutoStartup(this.autoStartup);
		adapter.setPhase(this.phase);
		if (this.errorChannel != null) {
			adapter.setErrorChannel(this.errorChannel);
		}
		if (this.sendTimeout != null) {
			adapter.setSendTimeout(this.sendTimeout);
		}
		if (this.converter != null) {
			adapter.setConverter(this.converter);
		}
		if (this.beanName != null) {
			adapter.setBeanName(this.beanName);
		}
		if (this.getBeanFactory() != null) {
			adapter.setBeanFactory(this.getBeanFactory());
		}
		adapter.afterPropertiesSet();
		this.adapter = adapter;
		return adapter;
	}

}
