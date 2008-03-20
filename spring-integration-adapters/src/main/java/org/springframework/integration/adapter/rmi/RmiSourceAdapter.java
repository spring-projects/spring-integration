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

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.adapter.AbstractMessageHandlingSourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.remoting.rmi.RmiServiceExporter;

/**
 * A source channel adapter for RMI-based remoting.
 * 
 * @author Mark Fisher
 */
public class RmiSourceAdapter extends AbstractMessageHandlingSourceAdapter {

	public static final String SERVICE_NAME_PREFIX = "internal.rmiSourceAdapter.";


	public RmiSourceAdapter() {
		super();
	}

	public RmiSourceAdapter(MessageChannel channel) {
		this();
		this.setChannel(channel);
	}


	public void initialize() throws RemoteException {
		String channelName = this.getChannel().getName();
		if (channelName == null) {
			throw new MessagingConfigurationException("RmiSourceAdapter's MessageChannel must have a 'name'");
		}
		RmiServiceExporter exporter = new RmiServiceExporter();
		exporter.setService(this);
		exporter.setServiceInterface(MessageHandler.class);
		exporter.setServiceName(SERVICE_NAME_PREFIX + channelName);
		exporter.afterPropertiesSet();
	}

}
