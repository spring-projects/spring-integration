/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.xmpp.config;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.util.XmppStringUtils;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.StringUtils;

/**
 * This class configures an {@link XMPPTCPConnection} object.
 * This object is used for all scenarios to talk to a Smack server.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Florian Schmaus
 * @author Artem Bilan
 *
 * @see XMPPTCPConnection
 * @since 2.0
 */
public class XmppConnectionFactoryBean extends AbstractFactoryBean<XMPPConnection> implements SmartLifecycle {

	private final Object lifecycleMonitor = new Object();

	private XMPPTCPConnectionConfiguration connectionConfiguration;

	private volatile String resource; // server will generate resource if not provided

	private volatile String user;

	private volatile String password;

	private volatile String serviceName;

	private volatile String host;

	private volatile int port = 5222;

	private volatile Roster.SubscriptionMode subscriptionMode = Roster.getDefaultSubscriptionMode();

	private volatile boolean autoStartup = true;

	private volatile int phase = Integer.MIN_VALUE;

	private volatile boolean running;

	private volatile XMPPTCPConnection connection;


	public XmppConnectionFactoryBean() {
	}

	/**
	 * @param connectionConfiguration the {@link XMPPTCPConnectionConfiguration} to use.
	 * @deprecated since {@literal 4.2.5} in favor of {@link #setConnectionConfiguration(XMPPTCPConnectionConfiguration)}
	 * to avoid {@code BeanCurrentlyInCreationException}
	 * during {@code AbstractAutowireCapableBeanFactory.getSingletonFactoryBeanForTypeCheck()}
	 */
	public XmppConnectionFactoryBean(XMPPTCPConnectionConfiguration connectionConfiguration) {
		setConnectionConfiguration(connectionConfiguration);
	}

	/**
	 * @param connectionConfiguration the {@link XMPPTCPConnectionConfiguration} to use.
	 * @since 4.2.5
	 */
	public void setConnectionConfiguration(XMPPTCPConnectionConfiguration connectionConfiguration) {
		this.connectionConfiguration = connectionConfiguration;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setSubscriptionMode(Roster.SubscriptionMode subscriptionMode) {
		this.subscriptionMode = subscriptionMode;
	}

	@Override
	public Class<? extends XMPPConnection> getObjectType() {
		return XMPPConnection.class;
	}

	@Override
	protected XMPPConnection createInstance() throws Exception {
		XMPPTCPConnectionConfiguration connectionConfiguration = this.connectionConfiguration;
		if (this.connectionConfiguration == null) {
			XMPPTCPConnectionConfiguration.Builder builder =
					XMPPTCPConnectionConfiguration.builder()
							.setHost(this.host)
							.setPort(this.port)
							.setResource(this.resource)
							.setUsernameAndPassword(this.user, this.password)
							.setServiceName(this.serviceName);

			if (!StringUtils.hasText(this.serviceName) && StringUtils.hasText(this.user)) {
				builder.setUsernameAndPassword(XmppStringUtils.parseLocalpart(this.user), this.password)
						.setServiceName(XmppStringUtils.parseDomain(this.user));
			}

			connectionConfiguration = builder.build();
		}
		this.connection = new XMPPTCPConnection(connectionConfiguration);
		return this.connection;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			try {
				this.connection.connect();
				this.connection.addConnectionListener(new LoggingConnectionListener());
				this.connection.login();
				if (this.subscriptionMode != null) {
					Roster.getInstanceFor(this.connection).setSubscriptionMode(this.subscriptionMode);
				}
				this.running = true;
			}
			catch (Exception e) {
				throw new BeanInitializationException("failed to connect to XMPP service for "
						+ this.connection.getServiceName(), e);
			}
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.isRunning()) {
				this.connection.disconnect();
				this.running = false;
			}
		}
	}

	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	public boolean isRunning() {
		return this.running;
	}

	public int getPhase() {
		return this.phase;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}


	private class LoggingConnectionListener implements ConnectionListener {

		public void reconnectionSuccessful() {
			logger.debug("Reconnection successful");
		}

		public void reconnectionFailed(Exception e) {
			logger.debug("Reconnection failed", e);
		}

		public void reconnectingIn(int seconds) {
			logger.debug("Reconnecting in " + seconds + " seconds");
		}

		public void connectionClosedOnError(Exception e) {
			logger.debug("Connection closed on error", e);
		}

		public void connectionClosed() {
			logger.debug("Connection closed");
		}

		@Override
		public void connected(XMPPConnection connection) {
			logger.debug("Connection connected");
		}

		@Override
		public void authenticated(XMPPConnection connection, boolean resumed) {
			logger.debug("Connection authenticated");
		}

	}

}
