/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
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
	
    private static final BlockingQueue<Message<Collection<?>>> MESSAGES = new LinkedBlockingQueue<Message<Collection<?>>>();

    public void receive(Message<Collection<?>>message) {
    	LOGGER.info("Service Activator received Message: " + message);    	
    	MESSAGES.add(message);
    }

    public Message<Collection<?>> poll(long timeoutInMillis) throws InterruptedException {
        return MESSAGES.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

}
