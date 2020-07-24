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

package org.springframework.integration.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An inbound Messaging Gateway for RMI-based remoting.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @deprecated since 5.4 with no replacement.
 */
@Deprecated
public class RmiInboundGateway extends MessagingGatewaySupport
		implements RequestReplyExchanger {

	public static final String SERVICE_NAME_PREFIX = "org.springframework.integration.rmiGateway.";


	private final org.springframework.remoting.rmi.RmiServiceExporter exporter =
			new org.springframework.remoting.rmi.RmiServiceExporter();

	private String requestChannelName;

	private String registryHost;

	private int registryPort = Registry.REGISTRY_PORT;

	private boolean expectReply = true;

	private RemoteInvocationExecutor remoteInvocationExecutor;


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

	@Override
	public void setRequestChannelName(String requestChannelName) {
		this.requestChannelName = requestChannelName;
		super.setRequestChannelName(requestChannelName);
	}

	/**
	 * Specify whether the gateway should be expected to return a reply.
	 * The default is '<code>true</code>'.
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
	protected void onInit() {
		super.onInit();

		if (this.registryHost != null) {
			this.exporter.setRegistryHost(this.registryHost);
		}
		this.exporter.setRegistryPort(this.registryPort);
		if (this.remoteInvocationExecutor != null) {
			this.exporter.setRemoteInvocationExecutor(this.remoteInvocationExecutor);
		}
		this.exporter.setService(this);
		this.exporter.setServiceInterface(RequestReplyExchanger.class);
		this.exporter.setServiceName(SERVICE_NAME_PREFIX + this.requestChannelName);
		try {
			this.exporter.afterPropertiesSet();
		}
		catch (RemoteException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	@Nullable
	public Message<?> exchange(Message<?> message) {
		if (this.expectReply) {
			return sendAndReceiveMessage(message);
		}
		else {
			send(message);
			return null;
		}
	}

	@Override
	public void destroy() {
		try {
			super.destroy();
			this.exporter.destroy();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
