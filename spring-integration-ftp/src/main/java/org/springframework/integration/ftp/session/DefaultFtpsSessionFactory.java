/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.session;

import java.io.IOException;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTPSClient;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.JavaUtils;
import org.springframework.util.StringUtils;

/**
 * SessionFactory for FTPS.
 *
 * @author Josh Long
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class DefaultFtpsSessionFactory extends AbstractFtpSessionFactory<FTPSClient> {

	private @Nullable Boolean useClientMode;

	private @Nullable Boolean sessionCreation;

	private @Nullable String authValue;

	private @Nullable TrustManager trustManager;

	private String @Nullable [] cipherSuites;

	private String @Nullable [] protocols;

	private @Nullable KeyManager keyManager;

	private @Nullable Boolean needClientAuth;

	private @Nullable Boolean wantsClientAuth;

	private boolean implicit = false;

	private String prot = "P";

	private @Nullable String protocol;

	private boolean endpointCheckingEnabled = true;

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setUseClientMode(Boolean useClientMode) {
		this.useClientMode = useClientMode;
	}

	public void setSessionCreation(Boolean sessionCreation) {
		this.sessionCreation = sessionCreation;
	}

	public void setAuthValue(String authValue) {
		this.authValue = authValue;
	}

	public void setTrustManager(TrustManager trustManager) {
		this.trustManager = trustManager;
	}

	public void setCipherSuites(String[] cipherSuites) {
		this.cipherSuites = Arrays.copyOf(cipherSuites, cipherSuites.length);
	}

	public void setProtocols(String[] protocols) {
		this.protocols = Arrays.copyOf(protocols, protocols.length);
	}

	public void setKeyManager(KeyManager keyManager) {
		this.keyManager = keyManager;
	}

	public void setNeedClientAuth(Boolean needClientAuth) {
		this.needClientAuth = needClientAuth;
	}

	public void setWantsClientAuth(Boolean wantsClientAuth) {
		this.wantsClientAuth = wantsClientAuth;
	}

	public void setProt(String prot) {
		this.prot = prot;
	}

	public void setImplicit(boolean implicit) {
		this.implicit = implicit;
	}

	/**
	 * Set whether the FTPS client must verify that the server's certificate matches the
	 * requested host name (via {@link FTPSClient#setEndpointCheckingEnabled(boolean)}).
	 * Defaults to {@code true}. Disabling this check should only be done when connecting to trusted internal
	 * servers that cannot present a certificate matching their host name.
	 * @param endpointCheckingEnabled whether to enable host name verification; default true.
	 * @since 5.5.23
	 */
	public void setEndpointCheckingEnabled(boolean endpointCheckingEnabled) {
		this.endpointCheckingEnabled = endpointCheckingEnabled;
	}

	@Override
	protected FTPSClient createClientInstance() {
		if (StringUtils.hasText(this.protocol)) {
			return new FTPSClient(this.protocol, this.implicit);
		}
		return new FTPSClient(this.implicit);
	}

	@Override
	protected void postProcessClientAfterConnect(FTPSClient ftpsClient) throws IOException {
		ftpsClient.execPBSZ(0);
		ftpsClient.execPROT(this.prot);
	}

	@Override
	protected void postProcessClientBeforeConnect(FTPSClient ftpsClient) {
		if (this.trustManager != null) {
			ftpsClient.setTrustManager(this.trustManager);
		}
		else {
			// Clear the date-only-validating default TrustManager that Commons Net's
			// FTPSClient installs out of the box so that the JRE default TrustManagerFactory is
			// used instead, providing full PKIX certificate chain validation.
			ftpsClient.setTrustManager(null);
		}
		ftpsClient.setEndpointCheckingEnabled(this.endpointCheckingEnabled);
		JavaUtils.INSTANCE
				.acceptIfHasText(this.authValue, ftpsClient::setAuthValue)
				.acceptIfNotNull(this.cipherSuites, ftpsClient::setEnabledCipherSuites)
				.acceptIfNotNull(this.protocols, ftpsClient::setEnabledProtocols)
				.acceptIfNotNull(this.sessionCreation, ftpsClient::setEnabledSessionCreation)
				.acceptIfNotNull(this.useClientMode, ftpsClient::setUseClientMode)
				.acceptIfNotNull(this.keyManager, ftpsClient::setKeyManager)
				.acceptIfNotNull(this.needClientAuth, ftpsClient::setNeedClientAuth)
				.acceptIfNotNull(this.wantsClientAuth, ftpsClient::setWantClientAuth);
	}

}
