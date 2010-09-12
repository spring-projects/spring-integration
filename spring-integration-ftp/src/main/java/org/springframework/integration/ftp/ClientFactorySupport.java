package org.springframework.integration.ftp;

import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;


/**
 * Factors out the client factory creaton
 *
 * @author Josh Long
 */
public class ClientFactorySupport {
    public static DefaultFtpsClientFactory ftpsClientFactory(String host, int port, String remoteDir, String user, String pw, int fileType, int clientMode, String prot, String protocol,
        String authValue, Boolean implicit, TrustManager trustManager, KeyManager keyManager, Boolean sessionCreation, Boolean useClientMode, Boolean wantsClientAuth, Boolean needClientAuth , String [] cipherSuites) {
        DefaultFtpsClientFactory defaultFtpClientFactory = new DefaultFtpsClientFactory();
        defaultFtpClientFactory.setHost(host);
        defaultFtpClientFactory.setPassword(pw);
        defaultFtpClientFactory.setPort((port));
        defaultFtpClientFactory.setRemoteWorkingDirectory(remoteDir);
        defaultFtpClientFactory.setUsername(user);
        defaultFtpClientFactory.setFileType(fileType);
        defaultFtpClientFactory.setClientMode(clientMode);

	    if(cipherSuites !=null)
	    defaultFtpClientFactory.setCipherSuites( cipherSuites );



        if (StringUtils.hasText(prot)) {
            defaultFtpClientFactory.setProt(prot);
        }

        if (StringUtils.hasText(protocol)) {
            defaultFtpClientFactory.setProtocol(protocol);
        }

        if (StringUtils.hasText(authValue)) {
            defaultFtpClientFactory.setAuthValue(authValue);
        }

        if (null != implicit) {
            defaultFtpClientFactory.setImplicit(implicit);
        }

        if (trustManager != null) {
            defaultFtpClientFactory.setTrustManager(trustManager);
        }

        if (keyManager != null) {
            defaultFtpClientFactory.setKeyManager(keyManager);
        }

        if (needClientAuth != null) {
            defaultFtpClientFactory.setNeedClientAuth(needClientAuth);
        }

        if (wantsClientAuth != null) {
            defaultFtpClientFactory.setWantsClientAuth(wantsClientAuth);
        }

        if (sessionCreation != null) {
            defaultFtpClientFactory.setSessionCreation(sessionCreation);
        }

        if (useClientMode != null) {
            defaultFtpClientFactory.setUseClientMode(useClientMode);
        }

        return defaultFtpClientFactory;
    }

    public static DefaultFtpClientFactory ftpClientFactory(String host, int port, String remoteDir, String user, String pw, int clientMode, int fileType) {
        DefaultFtpClientFactory defaultFtpClientFactory = new DefaultFtpClientFactory();
        defaultFtpClientFactory.setHost(host);
        defaultFtpClientFactory.setPassword(pw);
        defaultFtpClientFactory.setPort(port);
        defaultFtpClientFactory.setRemoteWorkingDirectory(remoteDir);
        defaultFtpClientFactory.setUsername(user);
        defaultFtpClientFactory.setClientMode(clientMode);
	    defaultFtpClientFactory.setFileType( fileType );

        return defaultFtpClientFactory;
    }
}
