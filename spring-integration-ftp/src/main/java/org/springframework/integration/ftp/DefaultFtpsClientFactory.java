package org.springframework.integration.ftp;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

import org.springframework.beans.factory.config.PropertiesFactoryBean;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.SocketException;

import java.security.NoSuchAlgorithmException;

import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;


/**
 * provides a working FTPS implementation
 *
 * @author Josh Long
 */
public class DefaultFtpsClientFactory extends AbstractFtpClientFactory<FTPSClient> {
    private Boolean useClientMode;
    private Boolean sessionCreation;
    private String authValue;
    private TrustManager trustManager;
    private String[] cipherSuites;
    private String[] protocols;
    private KeyManager keyManager;
    private Boolean needClientAuth;
    private Boolean wantsClientAuth;
    private boolean implicit = false;
    private String prot = "P";
    private String protocol;

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static void main(String[] args) throws Throwable {
        File file = new File(SystemUtils.getUserHome(), "Desktop/ftp.properties");
        Resource r = new FileSystemResource(file);
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(r);
        propertiesFactoryBean.afterPropertiesSet();

        Properties props = propertiesFactoryBean.getObject();

        String user = props.getProperty("ftp.username");
        String pw = props.getProperty("ftp.password");
        String host = props.getProperty("ftp.host");

        if (!file.exists()) {
            throw new RuntimeException("doesn't exist");
        }

        DefaultFtpsClientFactory defaultFtpsClientFactory = new DefaultFtpsClientFactory();
        defaultFtpsClientFactory.setUsername(user);
        defaultFtpsClientFactory.setImplicit(false);
        defaultFtpsClientFactory.setPassword(pw);
        defaultFtpsClientFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        defaultFtpsClientFactory.setHost(host);

        FTPSClient ftpClient = defaultFtpsClientFactory.getClient();

        InputStream fileStream = r.getInputStream();
        ftpClient.storeFile("pushed.java", fileStream);
        fileStream.close();
        ftpClient.disconnect();
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
        this.cipherSuites = cipherSuites;
    }

    public void setProtocols(String[] protocols) {
        this.protocols = protocols;
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

    @Override
    protected void onAfterConnect(FTPSClient ftpsClient)
        throws IOException {
        ftpsClient.execPBSZ(0);
        ftpsClient.execPROT(this.prot);
    }

    @Override
    public FTPSClient getClient() throws SocketException, IOException {
        FTPSClient ftpsClient = super.getClient();

        if (StringUtils.hasText(this.authValue)) {
            ftpsClient.setAuthValue(authValue);
        }

        if (this.trustManager != null) {
            ftpsClient.setTrustManager(this.trustManager);
        }

        if (this.cipherSuites != null) {
            ftpsClient.setEnabledCipherSuites(this.cipherSuites);
        }

        if (this.protocols != null) {
            ftpsClient.setEnabledProtocols(this.protocols);
        }

        if (this.sessionCreation != null) {
            ftpsClient.setEnabledSessionCreation(this.sessionCreation);
        }

        if (this.useClientMode != null) {
            ftpsClient.setUseClientMode(this.useClientMode);
        }

        if (this.sessionCreation != null) {
            ftpsClient.setEnabledSessionCreation(this.sessionCreation);
        }

        if (this.keyManager != null) {
            ftpsClient.setKeyManager(keyManager);
        }

        if (this.needClientAuth != null) {
            ftpsClient.setNeedClientAuth(this.needClientAuth);
        }

        if (this.wantsClientAuth != null) {
            ftpsClient.setWantClientAuth(this.wantsClientAuth);
        }

        return ftpsClient;
    }

    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
    }

    @Override
    protected FTPSClient createSingleInstanceOfClient() {
        try {
            if (StringUtils.hasText(this.protocol)) {
                return new FTPSClient(this.protocol, this.implicit);
            }

            return new FTPSClient(this.implicit);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
