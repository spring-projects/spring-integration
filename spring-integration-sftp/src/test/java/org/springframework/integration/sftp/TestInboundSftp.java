package org.springframework.integration.sftp;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Josh Long
 */
public class TestInboundSftp {

    static public void main(String [] args) throws Throwable {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("TestInboundSftp.xml");
        classPathXmlApplicationContext.start();
    }
}
