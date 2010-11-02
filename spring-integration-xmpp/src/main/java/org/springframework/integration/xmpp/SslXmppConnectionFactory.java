package org.springframework.integration.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;


/**
 * An extension of {@link org.springframework.integration.xmpp.XmppConnectionFactory} that handles factorying a secure XMPP connection factory.
 * <p/>
 * This is interchangeable with existing {@link org.jivesoftware.smack.XMPPConnection} references, of course.
 *
 * @author Josh Long
 */
public class SslXmppConnectionFactory extends XmppConnectionFactory {
	private volatile String trustStorePassword;
	private volatile String trustStoreType;
	private volatile Resource trustStore;
	private volatile boolean securityEnabled = true;
	private volatile SocketFactory socketFactory;
	private volatile ConnectionConfiguration.SecurityMode securityMode = ConnectionConfiguration.SecurityMode.enabled;

	/**
	 * This method is a callback provided by the super type that lets us plugin to the configuration mechanism
	 *
	 * @param connectionConfiguration the connection Configuration
	 * @throws Exception
	 */

	@Override
	protected void setupConnectionConfiguration(ConnectionConfiguration connectionConfiguration) throws Exception {

		this.securityEnabled = securityMode != null &&
				(ConnectionConfiguration.SecurityMode.enabled.equals(securityMode) ||
						ConnectionConfiguration.SecurityMode.required.equals(securityMode));

		if (this.securityEnabled) {
			if (this.socketFactory == null) {
				this.socketFactory = SSLSocketFactory.getDefault();
			}

			Assert.notNull(this.trustStore, "'trustStore' must not be null");

			String trustStorePath = this.trustStore.toString();
			connectionConfiguration.setTruststorePath(trustStorePath);

			// not required
			if (StringUtils.hasText(this.trustStorePassword))
				connectionConfiguration.setTruststorePassword(this.trustStorePassword);

			// not required
			if (StringUtils.hasText(this.trustStoreType))
				connectionConfiguration.setTruststoreType(this.trustStoreType);
		}
	}

	/**
	 * Not required. If not specified, we will load reference using {@link javax.net.ssl.SSLSocketFactory#getDefault()}
	 *
	 * @param socketFactory the socket factory to be passed to the {@link org.jivesoftware.smack.ConnectionConfiguration}
	 */
	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	/**
	 * the password to use to access the trust store (optional)
	 *
	 * @param trustStorePassword
	 */
	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	/**
	 * This is required and specifies the path to the keystore (ie: /path/to/foo.jks)
	 *
	 * @param trustStore a {@link org.springframework.core.io.Resource} to the path itself
	 */
	public void setTrustStore(Resource trustStore) {
		this.trustStore = trustStore;
	}

	/**
	 * the type of trust store.
	 *
	 * @param trustStoreType the type of trust store
	 */
	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}


	/**
	 * used on {@link org.jivesoftware.smack.ConnectionConfiguration#setSecurityMode(org.jivesoftware.smack.ConnectionConfiguration.SecurityMode)}
	 * <p/>
	 * basically, if you're using this class we assume you want security enabled. If you don't you can always override it by specifying {@link org.jivesoftware.smack.ConnectionConfiguration.SecurityMode#disabled}
	 */
	public void setSecurityMode(ConnectionConfiguration.SecurityMode securityMode) {
		this.securityMode = securityMode;
	}
}
