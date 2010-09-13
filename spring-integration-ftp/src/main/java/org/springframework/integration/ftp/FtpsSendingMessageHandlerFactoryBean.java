package org.springframework.integration.ftp;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

public class FtpsSendingMessageHandlerFactoryBean extends FtpSendingMessageHandlerFactoryBean {
	/**
	   * Sets whether the connection is implicit. Local testing reveals this to be a good choice.
	   */
	  protected volatile Boolean implicit = Boolean.FALSE;

	  /**
	   * "TLS" or "SSL"
	   */
	  protected volatile String protocol;




	  /**
	   * "P"
	   */
	  protected volatile String prot;
	  private KeyManager keyManager;
	  private TrustManager trustManager;
	  protected volatile String authValue;
	  private Boolean sessionCreation;
	  private Boolean useClientMode;
	  private Boolean needClientAuth;
	  private Boolean wantsClientAuth;
	  private String[] cipherSuites;

	public void setImplicit(Boolean implicit) {
		this.implicit = implicit;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setProt(String prot) {
		this.prot = prot;
	}

	public void setKeyManager(KeyManager keyManager) {
		this.keyManager = keyManager;
	}

	public void setTrustManager(TrustManager trustManager) {
		this.trustManager = trustManager;
	}

	public void setAuthValue(String authValue) {
		this.authValue = authValue;
	}

	public void setSessionCreation(Boolean sessionCreation) {
		this.sessionCreation = sessionCreation;
	}

	public void setUseClientMode(Boolean useClientMode) {
		this.useClientMode = useClientMode;
	}

	public void setNeedClientAuth(Boolean needClientAuth) {
		this.needClientAuth = needClientAuth;
	}

	public void setWantsClientAuth(Boolean wantsClientAuth) {
		this.wantsClientAuth = wantsClientAuth;
	}

	public void setCipherSuites(String[] cipherSuites) {
		this.cipherSuites = cipherSuites;
	}

	private int fileType ;

	public void setFileType(int fileType) {
		this.fileType = fileType;
	}

	@Override
	protected AbstractFtpClientFactory clientFactory() {
		DefaultFtpsClientFactory factory = ClientFactorySupport.ftpsClientFactory(
				this.host,  (this.port), this.remoteDirectory, this.username, this.password, this.fileType,
		              this.clientMode, this.prot, this.protocol, this.authValue, this.implicit, this.trustManager, this.keyManager, this.sessionCreation, this.useClientMode, this.wantsClientAuth,
		              this.needClientAuth, this.cipherSuites);

		return factory;
	}
}
