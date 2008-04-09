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

package org.springframework.integration.adapter.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.MessageHandlingSourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.springframework.remoting.support.RemoteInvocationExecutor;

/**
 * A source channel adapter for RMI-based remoting.
 * 
 * @author Mark Fisher
 */
public class RmiSourceAdapter extends MessageHandlingSourceAdapter {

	public static final String SERVICE_NAME_PREFIX = "internal.rmiSourceAdapter.";


	private volatile String registryHost;

	private volatile int registryPort = Registry.REGISTRY_PORT;

	private volatile RemoteInvocationExecutor remoteInvocationExecutor;


	public RmiSourceAdapter() {
		super();
	}

	public RmiSourceAdapter(MessageChannel channel) {
		this();
		this.setChannel(channel);
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

	public void initialize() throws RemoteException {
		String channelName = this.getChannel().getName();
		if (channelName == null) {
			throw new ConfigurationException("RmiSourceAdapter's MessageChannel must have a 'name'");
		}
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
		exporter.setServiceName(SERVICE_NAME_PREFIX + channelName);
		exporter.afterPropertiesSet();
	}

}
