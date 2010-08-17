package org.springframework.integration.ftp;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This simple example demonstrates sending a file to a remote FTP server using the ftp:outbound-channel-adapter
 *
 * It reads files from a directory on your computer and systematically puts them on the remote FTP server,
 *
 * @author Josh Long
 */
public class OutboundFTPExample {
    public static void main(String [] args ) throws Throwable {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("outbound-ftp-context.xml");
    }
}
