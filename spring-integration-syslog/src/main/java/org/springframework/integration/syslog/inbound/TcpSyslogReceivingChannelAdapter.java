/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.syslog.inbound;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;
import org.springframework.messaging.Message;

/**
 * TCP implementation of a syslog inbound channel adapter.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class TcpSyslogReceivingChannelAdapter extends SyslogReceivingChannelAdapterSupport
		implements TcpListener, ApplicationEventPublisherAware {

	private volatile AbstractServerConnectionFactory connectionFactory;

	private volatile ApplicationEventPublisher applicationEventPublisher;

	/**
	 * @param connectionFactory The connection factory.
	 */
	public void setConnectionFactory(AbstractServerConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected int getPort() {
		if (this.connectionFactory == null) {
			return super.getPort();
		}
		else {
			return this.connectionFactory.getPort();
		}
	}

	@Override
	public String getComponentType() {
		return "syslog:inbound-channel-adapter(tcp)";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.connectionFactory == null) {
			this.connectionFactory = new TcpNioServerConnectionFactory(getPort());
			this.connectionFactory.setDeserializer(new ByteArrayLfSerializer());
			this.connectionFactory.setBeanFactory(getBeanFactory());
			if (this.applicationEventPublisher != null) {
				this.connectionFactory.setApplicationEventPublisher(this.applicationEventPublisher);
			}
			this.connectionFactory.afterPropertiesSet();
		}
		this.connectionFactory.registerListener(this);
	}

	@Override
	protected void doStart() {
		this.connectionFactory.start();
	}

	@Override
	protected void doStop() {
		this.connectionFactory.stop();
	}

	@Override
	public boolean onMessage(Message<?> message) {
		convertAndSend(message);
		return false;
	}

}
