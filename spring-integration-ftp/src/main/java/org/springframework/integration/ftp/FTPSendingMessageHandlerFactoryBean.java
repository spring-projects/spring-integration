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
public class FTPSendingMessageHandlerFactoryBean extends AbstractFactoryBean<FTPSendingMessageHandler> implements ResourceLoaderAware, ApplicationContextAware {
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
    public Class<?extends FTPSendingMessageHandler> getObjectType() {
        return FTPSendingMessageHandler.class;
    }

    @Override
    protected FTPSendingMessageHandler createInstance()
        throws Exception {
        // the dependencies for the outbound-adapter are much simpler
        // they only require an instance of the pool
        DefaultFTPClientFactory defaultFTPClientFactory = new DefaultFTPClientFactory();
        defaultFTPClientFactory.setHost(this.host);
        defaultFTPClientFactory.setPassword(this.password);
        defaultFTPClientFactory.setPort(this.port);
        defaultFTPClientFactory.setRemoteWorkingDirectory(this.remoteDirectory);
        defaultFTPClientFactory.setUsername(this.username);
        defaultFTPClientFactory.setClientMode(this.clientMode);

        QueuedFTPClientPool queuedFTPClientPool = new QueuedFTPClientPool(15, defaultFTPClientFactory);

        FTPSendingMessageHandler ftpSendingMessageHandler = new FTPSendingMessageHandler(queuedFTPClientPool);
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
