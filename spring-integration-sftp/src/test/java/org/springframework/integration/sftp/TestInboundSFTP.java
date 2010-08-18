package org.springframework.integration.sftp;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Josh Long
 */
public class TestInboundSFTP {

    static public void main(String [] args) throws Throwable {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("TestInboundSFTP.xml");
        classPathXmlApplicationContext.start();
    }
}
