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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.StringUtils;

/**
 * This class configures an {@link org.jivesoftware.smack.XMPPConnection} object. This object is used for all scenarios to talk to a Smack server.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @see {@link org.jivesoftware.smack.XMPPConnection}
 * @since 2.0
 */
public class XmppConnectionFactory extends AbstractFactoryBean<XMPPConnection> {
    // TODO provide a default subscription mode for this class: Roster.setSubscriptionMode(Roster.SubscriptionMode) 
    private static final Log logger = LogFactory.getLog(XmppConnectionFactory.class);

    private volatile String user;

    private volatile String password;

    private volatile String host;

    private volatile String serviceName;

    private volatile String resource;

    private volatile String saslMechanismSupported;

    private volatile int saslMechanismSupportedIndex;

    private volatile int port;

    private volatile String subscriptionMode;


    private volatile boolean debug = false;

    public XmppConnectionFactory() {
    }

    public XmppConnectionFactory(String user, String password, String host, String serviceName, String resource, String saslMechanismSupported, int saslMechanismSupportedIndex, int port) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.serviceName = serviceName;
        this.resource = resource;
        this.saslMechanismSupported = saslMechanismSupported;
        this.saslMechanismSupportedIndex = saslMechanismSupportedIndex;
        this.port = port;
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getSaslMechanismSupported() {
        return saslMechanismSupported;
    }

    public void setSaslMechanismSupported(String saslMechanismSupported) {
        this.saslMechanismSupported = saslMechanismSupported;
    }

    public int getSaslMechanismSupportedIndex() {
        return saslMechanismSupportedIndex;
    }

    public void setSaslMechanismSupportedIndex(int saslMechanismSupportedIndex) {
        this.saslMechanismSupportedIndex = saslMechanismSupportedIndex;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        XMPPConnection.DEBUG_ENABLED = debug;
        this.debug = debug;
    }


    public void setSubscriptionMode(final String subscriptionMode) {
        this.subscriptionMode = subscriptionMode;
    }

    @Override
    public Class<?extends XMPPConnection> getObjectType() {
        return XMPPConnection.class;
    }

    private XMPPConnection configureAndConnect(String usr, String pw, String host, int port, String serviceName, String resource, String saslMechanismSupported, int saslMechanismSupportedIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("usr=%s, pw=%s, host=%s, port=%s, serviceName=%s, resource=%s, saslMechanismSupported=%s, saslMechanismSupportedIndex=%s", usr, pw, host, port, serviceName,
                    resource, saslMechanismSupported, saslMechanismSupportedIndex));
        }

        XMPPConnection.DEBUG_ENABLED = false; // default

        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(host, port, serviceName);
        XMPPConnection connection = new XMPPConnection(connectionConfiguration);

        try {
            connection.connect();

            // You have to put this code before you login
            if (StringUtils.hasText(saslMechanismSupported)) {
                SASLAuthentication.supportSASLMechanism(saslMechanismSupported, saslMechanismSupportedIndex);
            }

            // You have to specify the resoure (e.g. "@host.com") at the end
            if (StringUtils.hasText(resource)) {
                connection.login(usr, pw, resource);
            } else {
                connection.login(usr, pw);
            }

            if( StringUtils.hasText( this.subscriptionMode)) {
                Roster.SubscriptionMode subscriptionMode = Roster.SubscriptionMode.valueOf( this.subscriptionMode) ;
                connection.getRoster().setSubscriptionMode( subscriptionMode );
            }

            if (logger.isDebugEnabled()) {
                logger.debug("authenticated? " + connection.isAuthenticated());
            }
        } catch (Exception e) {
            logger.warn("failed to establish XMPP connnection", e);
        }

        return connection;
    }



    @Override
    protected XMPPConnection createInstance() throws Exception {
        return this.configureAndConnect(this.getUser(), this.getPassword(), this.getHost(), this.getPort(), this.getServiceName(), this.getResource(), this.getSaslMechanismSupported(),
            this.getSaslMechanismSupportedIndex());
    }
}
