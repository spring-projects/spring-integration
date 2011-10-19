/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.jdbc.storedproc.CreateUser;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StoredProcOutboundGatewayWithSpringContextIntegrationTests {

    private static Logger logger = Logger.getLogger(StoredProcOutboundGatewayWithSpringContextIntegrationTests.class);

    @Autowired
    private DataSource datasource;

    @Autowired
    private AbstractApplicationContext context;

    @Autowired
    private Consumer consumer;

    @Autowired
    CreateUser createUser;
    
    @Test
    public void test() throws Exception {
    	
    	createUser.createUser(new User("myUsername", "myPassword", "myEmail"));
    	
        List<Message<?>> received = new ArrayList<Message<?>>();

        received.add(consumer.poll(2000));

        Message<?> message = received.get(0);
        context.stop();
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertNotNull(message.getPayload() instanceof Collection<?>);

        Collection<User> allUsers = (Collection<User>) message.getPayload();

        assertTrue(allUsers.size() == 1);

    }

    static class Counter {

        private final AtomicInteger count = new AtomicInteger();

        public Integer next() throws InterruptedException {
            if (count.get()>2){
                //prevent message overload
                return null;
            }
            return Integer.valueOf(count.incrementAndGet());
        }
    }


    static class Consumer {

        private final BlockingQueue<Message<?>> messages = new LinkedBlockingQueue<Message<?>>();

        @ServiceActivator
        public void receive(Message<?>message) {
            messages.add(message);
        }

        Message<?> poll(long timeoutInMillis) throws InterruptedException {
            return messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
        }
    }
}
