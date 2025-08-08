/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.syslog.inbound;

import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;

/**
 * UDP implementation of a syslog inbound channel adapter.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class UdpSyslogReceivingChannelAdapter extends SyslogReceivingChannelAdapterSupport {

	private volatile UnicastReceivingChannelAdapter udpAdapter;

	private volatile boolean udpAdapterSet;

	public void setUdpAdapter(UnicastReceivingChannelAdapter udpAdapter) {
		this.udpAdapter = udpAdapter;
		this.udpAdapterSet = true;
	}

	@Override
	protected int getPort() {
		if (this.udpAdapter == null) {
			return super.getPort();
		}
		else {
			return this.udpAdapter.getPort();
		}
	}

	@Override
	public String getComponentType() {
		return "syslog:inbound-channel-adapter(udp)";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.udpAdapter == null) {
			this.udpAdapter = new UnicastReceivingChannelAdapter(getPort());
			this.udpAdapter.setBeanFactory(getBeanFactory());
		}
		else {
			logger.info("The 'UdpSyslogReceivingChannelAdapter' overrides an 'outputChannel' " +
					"of the provided 'UnicastReceivingChannelAdapter' to support Syslog conversion " +
					"for the incoming UDP packets");
		}
		this.udpAdapter.setOutputChannel(new FixedSubscriberChannel(this::convertAndSend));
		if (!this.udpAdapterSet) {
			this.udpAdapter.afterPropertiesSet();
		}
	}

	@Override
	protected void doStart() {
		this.udpAdapter.start();
	}

	@Override
	protected void doStop() {
		this.udpAdapter.stop();
	}

}
