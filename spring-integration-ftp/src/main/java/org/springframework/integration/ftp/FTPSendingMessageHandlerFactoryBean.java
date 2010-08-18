package org.springframework.integration.ftp;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;


/**
 * A factory bean implementation that handles constructing an outbound FTP adapter.
 *
 * @author Josh Long
 */
public class FTPSendingMessageHandlerFactoryBean extends AbstractFactoryBean<FtpSendingMessageHandler> implements ResourceLoaderAware, ApplicationContextAware {
    private int port;
    private String username;
    private String password;
    private String host;
    private String remoteDirectory;
    private int clientMode;

    // private vars 
    private ResourceLoader resourceLoader;
    private ApplicationContext applicationContext;

    public void setClientMode(int clientMode) {
        this.clientMode = clientMode;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Class<?extends FtpSendingMessageHandler> getObjectType() {
        return FtpSendingMessageHandler.class;
    }

    @Override
    protected FtpSendingMessageHandler createInstance()
        throws Exception {
        // the dependencies for the outbound-adapter are much simpler
        // they only require an instance of the pool
        DefaultFtpClientFactory defaultFtpClientFactory = new DefaultFtpClientFactory();
        defaultFtpClientFactory.setHost(this.host);
        defaultFtpClientFactory.setPassword(this.password);
        defaultFtpClientFactory.setPort(this.port);
        defaultFtpClientFactory.setRemoteWorkingDirectory(this.remoteDirectory);
        defaultFtpClientFactory.setUsername(this.username);
        defaultFtpClientFactory.setClientMode(this.clientMode);

        QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15, defaultFtpClientFactory);

        FtpSendingMessageHandler ftpSendingMessageHandler = new FtpSendingMessageHandler(queuedFtpClientPool);
        ftpSendingMessageHandler.afterPropertiesSet();

        return ftpSendingMessageHandler;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setRemoteDirectory(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }
}
