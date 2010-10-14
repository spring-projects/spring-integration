package org.springframework.integration.ftp.impl;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import org.springframework.context.ResourceLoaderAware;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;

import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.integration.file.entries.PatternMatchingEntryListFilter;
import org.springframework.integration.ftp.*;

import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;


/**
 * Factory to make building the namespace easier
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpsRemoteFileSystemSynchronizingMessageSourceFactoryBean extends FtpRemoteFileSystemSynchronizingMessageSourceFactoryBean {
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

    public FtpsRemoteFileSystemSynchronizingMessageSourceFactoryBean() {
        this.defaultFtpInboundFolderName = "ftpsInbound";
        this.clientMode = FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE;
    }

    public void setKeyManager(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public void setTrustManager(TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    public void setImplicit(Boolean implicit) {
        this.implicit = implicit;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setProt(String prot) {
        this.prot = prot;
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

    protected AbstractFtpClientFactory defaultClientFactory()
        throws Exception {
        DefaultFtpsClientFactory factory = ClientFactorySupport.ftpsClientFactory(this.host, Integer.parseInt(this.port), this.remoteDirectory, this.username, this.password, this.fileType,
                this.clientMode, this.prot, this.protocol, this.authValue, this.implicit, this.trustManager, this.keyManager, this.sessionCreation, this.useClientMode, this.wantsClientAuth,
                this.needClientAuth, this.cipherSuites);

        return factory;
    }

    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites;
    }
}
