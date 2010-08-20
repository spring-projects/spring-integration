package org.springframework.integration.sftp;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

import java.io.File;
import java.util.logging.Logger;


/**
 * This tests the API, more than the end-to-end XML to Java approach.
 *
 * @author Josh Long
 */
public class TestSftpReceipt {
/*    
    private static final Logger logger = Logger.getLogger(TestSftpReceipt.class.getName());
    private SftpSessionFactory sftpSessionFactory;
    private String host;
    private String password;
    private String user;
    private String privateKeyPath;
    private String privateKeyPassword;
    private int port;

    @Before
    public void before() throws Throwable {
        this.sftpSessionFactory = buildSFTPSessionFactory(this.host, this.password, this.user, this.privateKeyPath, this.privateKeyPassword, this.port);
    }

    @Test
    public void testReceive() throws Throwable {
        String localMount = SystemUtils.getUserHome() + "/local_mount";
        String remoteMount = "remote_mount";

        // local path
        File local = new File(localMount); // obviously this is just for test. Do what you need to do in your own

        // we are testing, after all
        if (local.exists() && (local.list().length > 0)) {
            for (File f : local.listFiles()) {
                if (!f.delete()) {
                    logger.fine("couldn't delete " + f.getAbsolutePath());
                }
            }
        }

        Resource localDirectory = new FileSystemResource(local);

        // pool
        QueuedSftpSessionPool queuedSFTPSessionPool = new QueuedSftpSessionPool(sftpSessionFactory);
        queuedSFTPSessionPool.afterPropertiesSet();

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setErrorHandler(new ErrorHandler() {
                public void handleError(Throwable t) {
                    System.out.println("Error occurred: " + ExceptionUtils.getFullStackTrace(t));
                }
            });

        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.initialize();

        // synchronizer
        final SftpInboundSynchronizer sftpInboundSynchronizer = new SftpInboundSynchronizer();
        sftpInboundSynchronizer.setLocalDirectory(localDirectory);
        sftpInboundSynchronizer.setRemotePath(remoteMount);
        sftpInboundSynchronizer.setAutoCreatePath(true);
        sftpInboundSynchronizer.setPool(queuedSFTPSessionPool);
        sftpInboundSynchronizer.setShouldDeleteDownloadedRemoteFiles(false);
        sftpInboundSynchronizer.setTaskScheduler(taskScheduler);
        sftpInboundSynchronizer.afterPropertiesSet();
        sftpInboundSynchronizer.start();

        new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(60 * 1000); // 1 minute

                        sftpInboundSynchronizer.stop();
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
            }).start();
    }

    private SftpSessionFactory buildSFTPSessionFactory(String host, String pw, String usr, String pvKey, String pvKeyPass, int port)
        throws Throwable {
        SftpSessionFactory sftpSessionFactory = new SftpSessionFactory();
        sftpSessionFactory.setPassword(pw);
        sftpSessionFactory.setPort(port);
        sftpSessionFactory.setRemoteHost(host);
        sftpSessionFactory.setUser(usr);
        sftpSessionFactory.setPrivateKey(pvKey);
        sftpSessionFactory.setPrivateKeyPassphrase(pvKeyPass);
        sftpSessionFactory.afterPropertiesSet();

        return sftpSessionFactory;
    }*/
}
