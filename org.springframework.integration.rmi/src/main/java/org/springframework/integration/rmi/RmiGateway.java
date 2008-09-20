/*
 * Copyright 2002-2008 the original author or authors.
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

import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.adapter.AbstractRemotingGateway;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A gateway adapter for RMI-based remoting.
 * 
 * @author Mark Fisher
 */
public class RmiGateway extends AbstractRemotingGateway implements InitializingBean, MessageHandler {

	public static final String SERVICE_NAME_PREFIX = "org.springframewok.integration.rmiGateway.";


	private final String requestChannelName;

	private volatile String registryHost;

	private volatile int registryPort = Registry.REGISTRY_PORT;

	private volatile RemoteInvocationExecutor remoteInvocationExecutor;


	/**
	 * Create an RmiGateway that sends to the provided request channel.
	 * 
	 * @param requestChannel the channel where messages will be sent, must not be
	 * <code>null</code>.
	 */
	public RmiGateway(MessageChannel requestChannel) {
		super(requestChannel);
		this.requestChannelName = requestChannel.getName();
		Assert.isTrue(StringUtils.hasText(this.requestChannelName), "RmiGateway's request channel must have a name.");
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

	public void afterPropertiesSet() throws RemoteException {
		RmiServiceExporter exporter = new RmiServiceExporter();
		if (this.registryHost != null) {
			exporter.setRegistryHost(this.registryHost);
		}
		exporter.setRegistryPort(this.registryPort);
		if (this.remoteInvocationExecutor != null) {
			exporter.setRemoteInvocationExecutor(this.remoteInvocationExecutor);
		}
		exporter.setService(this);
		exporter.setServiceInterface(MessageHandler.class);
		exporter.setServiceName(SERVICE_NAME_PREFIX + this.requestChannelName);
		exporter.afterPropertiesSet();
	}

}
