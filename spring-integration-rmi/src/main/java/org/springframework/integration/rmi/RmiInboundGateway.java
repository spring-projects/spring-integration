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

package org.springframework.integration.rmi;

import java.rmi.registry.Registry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An inbound Messaging Gateway for RMI-based remoting.
 *
 * @author Mark Fisher
 */
public class RmiInboundGateway extends MessagingGatewaySupport implements RequestReplyExchanger, InitializingBean {

	public static final String SERVICE_NAME_PREFIX = "org.springframework.integration.rmiGateway.";


	private volatile String requestChannelName;

	private volatile String registryHost;

	private volatile int registryPort = Registry.REGISTRY_PORT;

	private volatile boolean expectReply = true;

	private volatile RemoteInvocationExecutor remoteInvocationExecutor;

	private volatile RmiServiceExporter exporter;

	private final Object initializationMonitor = new Object();


	/**
	 * Specify the request channel where messages will be sent.
	 * It must not be <code>null</code>, and it must have a name.
	 */
	@Override
	public void setRequestChannel(MessageChannel requestChannel) {
		Assert.notNull(requestChannel, "requestChannel must not be null");
		Assert.isTrue(requestChannel instanceof NamedComponent &&
				StringUtils.hasText(((NamedComponent) requestChannel).getComponentName()),
				"RmiGateway's request channel must have a name.");
		this.requestChannelName = ((NamedComponent) requestChannel).getComponentName();
		super.setRequestChannel(requestChannel);
	}

	/**
	 * Specify whether the gateway should be expected to return a reply.
	 * The default is '<code>true</code>'.
	 *
	 * @param expectReply true when a reply is expected.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	public void setRemoteInvocationExecutor(RemoteInvocationExecutor remoteInvocationExecutor) {
		this.remoteInvocationExecutor = remoteInvocationExecutor;
	}

	@Override
	public String getComponentType() {
		return "rmi:inbound-gateway";
	}

	@Override
	protected void onInit() throws Exception {
		synchronized (this.initializationMonitor) {
			if (this.exporter == null) {
				RmiServiceExporter exporter = new RmiServiceExporter();
				if (this.registryHost != null) {
					exporter.setRegistryHost(this.registryHost);
				}
				exporter.setRegistryPort(this.registryPort);
				if (this.remoteInvocationExecutor != null) {
					exporter.setRemoteInvocationExecutor(this.remoteInvocationExecutor);
				}
				exporter.setService(this);
				exporter.setServiceInterface(RequestReplyExchanger.class);
				exporter.setServiceName(SERVICE_NAME_PREFIX + this.requestChannelName);
				exporter.afterPropertiesSet();
				this.exporter = exporter;
			}
		}
		super.onInit();
	}

	@Override
	public Message<?> exchange(Message<?> message) {
		if (this.expectReply) {
			return this.sendAndReceiveMessage(message);
		}
		this.send(message);
		return null;
	}

}
