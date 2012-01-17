package org.springframework.integration.jpa.test;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.integration.Message;

/**
 * 
 * @author Gunnar Hillert
 * @since 2.2
 * 
 */
public final class Consumer {
	
	private static final Logger LOGGER = Logger.getLogger(Consumer.class);
	
    private static final BlockingQueue<Message<Collection<?>>> messages = new LinkedBlockingQueue<Message<Collection<?>>>();

    public void receive(Message<Collection<?>>message) {
    	LOGGER.info("Service Activator received Message: " + message);    	
        messages.add(message);
    }

    public Message<Collection<?>> poll(long timeoutInMillis) throws InterruptedException {
        return messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

}
