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
