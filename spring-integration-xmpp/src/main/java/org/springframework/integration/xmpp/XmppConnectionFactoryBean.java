/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This class configures an {@link org.jivesoftware.smack.XMPPConnection} object. 
 * This object is used for all scenarios to talk to a Smack server.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @see org.jivesoftware.smack.XMPPConnection
 * @since 2.0
 */
public class XmppConnectionFactoryBean extends AbstractFactoryBean<XMPPConnection> {

	private volatile ConnectionConfiguration connectionConfiguration;
	
	private volatile String resource = "Smack"; // default value used by Smack
	
	private volatile String user;
		
	private volatile String password;
	
	private volatile String subscriptionMode = "accept_all";

	public XmppConnectionFactoryBean(ConnectionConfiguration connectionConfiguration) {
		Assert.notNull(connectionConfiguration, "'connectionConfiguration' must not be null");
		this.connectionConfiguration = connectionConfiguration;
	}
	
	public String getSubscriptionMode() {
		return subscriptionMode;
	}

	public void setSubscriptionMode(String subscriptionMode) {
		this.subscriptionMode = subscriptionMode;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setResource(String resource) {
		this.resource = resource;
	}
	
	public String getResource() {
		return resource;
	}
	
	@Override
	public Class<? extends XMPPConnection> getObjectType() {
		return XMPPConnection.class;
	}

	@Override
	protected XMPPConnection createInstance() throws Exception {
		Assert.notNull(connectionConfiguration, "'connectionConfiguration' must not be null");
		
		XMPPConnection connection = new XMPPConnection(connectionConfiguration);
		connection.connect();
		if (StringUtils.hasText(user)){
			connection.login(user, password, resource);
			
			Assert.isTrue(connection.isAuthenticated(), "Failed to authenticate user: " + user);
			
			if (StringUtils.hasText(this.subscriptionMode)) {
				Roster.SubscriptionMode subscriptionMode = Roster.SubscriptionMode.valueOf(this.subscriptionMode);
				connection.getRoster().setSubscriptionMode(subscriptionMode);
			}	
		}
		else {
			connection.loginAnonymously();
		}
		
		return connection;
	}
}
