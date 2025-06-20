/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.xmpp.core;

import org.jivesoftware.smack.XMPPConnection;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
public abstract class AbstractXmppConnectionAwareMessageHandler extends AbstractMessageHandler {

	private XMPPConnection xmppConnection;

	private volatile boolean initialized;

	public AbstractXmppConnectionAwareMessageHandler() {
	}

	public AbstractXmppConnectionAwareMessageHandler(XMPPConnection xmppConnection) {
		Assert.notNull(xmppConnection, "XMPPConnection must not be null");
		this.xmppConnection = xmppConnection;
	}

	protected XMPPConnection getXmppConnection() {
		return this.xmppConnection;
	}

	@Override
	protected boolean isInitialized() {
		return this.initialized;
	}

	@Override
	protected void onInit() {
		BeanFactory beanFactory = this.getBeanFactory();
		if (this.xmppConnection == null && beanFactory != null) {
			this.xmppConnection =
					beanFactory.getBean(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, XMPPConnection.class);
		}
		Assert.notNull(this.xmppConnection, "Failed to resolve XMPPConnection. " +
				"XMPPConnection must either be set explicitly via constructor argument " +
				"or implicitly by registering a bean with the name 'xmppConnection' and of type " +
				"'org.jivesoftware.smack.XMPPConnection' in the Application Context.");
		this.initialized = true;
	}

}
