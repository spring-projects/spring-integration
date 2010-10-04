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
public class FtpSendingMessageHandlerFactoryBean extends AbstractFactoryBean<FtpSendingMessageHandler>
    implements ResourceLoaderAware, ApplicationContextAware {
    protected int port;
    protected String username;
    protected String password;
    protected String host;
    protected String remoteDirectory;
    private String charset;
    protected int clientMode;
    private int fileType;
	private ResourceLoader resourceLoader;
    private ApplicationContext applicationContext;

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

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

    protected AbstractFtpClientFactory clientFactory() {
        return ClientFactorySupport.ftpClientFactory(this.host, this.port,
            this.remoteDirectory, this.username, this.password,
            this.clientMode, this.fileType);
    }

    @Override
    protected FtpSendingMessageHandler createInstance()
        throws Exception {
        // the dependencies for the outbound-adapter are much simpler
        // they only require an instance of the pool
        AbstractFtpClientFactory defaultFtpClientFactory = clientFactory();

        QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15,
                defaultFtpClientFactory);

        FtpSendingMessageHandler ftpSendingMessageHandler = new FtpSendingMessageHandler(queuedFtpClientPool);
		ftpSendingMessageHandler.setCharset( this.charset);
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
