/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.syslog.inbound;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

/**
 * UDP implementation of a syslog inbound channel adapter.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public class UdpSyslogReceivingChannelAdapter extends SyslogReceivingChannelAdapterSupport {

	private volatile UnicastReceivingChannelAdapter udpAdapter;

	public void setUdpAdapter(UnicastReceivingChannelAdapter udpAdpter) {
		this.udpAdapter = udpAdpter;
	}

	@Override
	public String getComponentType() {
		return "syslog:inbound-channel-adapter(udp)";
	}

	@Override
	protected void onInit() {
		if (this.udpAdapter == null) {
			this.udpAdapter = new UnicastReceivingChannelAdapter(this.getPort());
		}
		DirectChannel outputChannel = new DirectChannel();
		outputChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				convertAndSend(message);
			}
		});
		this.udpAdapter.setOutputChannel(outputChannel);
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.udpAdapter.start();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.udpAdapter.stop();
	}

}
