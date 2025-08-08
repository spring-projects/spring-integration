/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.core;

import org.jivesoftware.smack.XMPPConnection;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public abstract class AbstractXmppConnectionAwareEndpoint extends MessageProducerSupport {

	private XMPPConnection xmppConnection;

	private boolean initialized;

	public AbstractXmppConnectionAwareEndpoint() {
	}

	public AbstractXmppConnectionAwareEndpoint(XMPPConnection xmppConnection) {
		Assert.notNull(xmppConnection, "'xmppConnection' must no be null");
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
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (this.xmppConnection == null && beanFactory != null) {
			this.xmppConnection = beanFactory.getBean(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, XMPPConnection.class);
		}
		Assert.notNull(this.xmppConnection, "Failed to resolve XMPPConnection. " +
				"XMPPConnection must either be set explicitly via constructor argument " +
				"or implicitly by registering a bean with the name 'xmppConnection' and of type " +
				"'org.jivesoftware.smack.XMPPConnection' in the Application Context.");
		this.initialized = true;
	}

}
