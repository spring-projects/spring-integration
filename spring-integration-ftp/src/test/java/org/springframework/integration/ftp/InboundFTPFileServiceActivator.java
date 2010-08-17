package org.springframework.integration.ftp;

import org.apache.commons.lang.StringUtils;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;

import org.springframework.stereotype.Component;

import java.io.File;


/**
 * Simple component to test the inbound integration
 *
 * @author Josh Long
 */

public class InboundFTPFileServiceActivator {

    @ServiceActivator
    public void onNewRemoteFTPFile(Message<File> file)
        throws Throwable {
        System.out.println(StringUtils.repeat("=", 100));
        System.out.println("A new file has appeared:  " + file.getPayload().getAbsolutePath());

        for (String h : file.getHeaders().keySet())
            System.out.println(String.format("%s = %s", h, file.getHeaders().get(h)));
    }

    public static void main(String[] args) throws Throwable {
        ClassPathXmlApplicationContext classPathXmlApplicationContext =
                new ClassPathXmlApplicationContext("inbound-ftp-context.xml");
    }
}
