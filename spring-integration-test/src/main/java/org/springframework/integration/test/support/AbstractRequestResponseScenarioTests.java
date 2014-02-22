/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.test.support;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Convenience class for testing Spring Integration request-response message scenarios. Users
 * create subclasses to execute on or more {@link RequestResponseScenario} tests. each scenario defines:
 * <ul>
 * <li>An inputChannelName</li>
 * <li>An outputChannelName</li>
 * <li>A payload or message to send as a request message on the inputChannel</li>
 * <li>A handler to validate the response received on the outputChannel</li>
 * </ul>
 * @author David Turanski
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractRequestResponseScenarioTests {

    private List<RequestResponseScenario> scenarios = null;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setUp(){
        scenarios = defineRequestResponseScenarios();
    }

    /**
     * Execute each scenario. Instantiate the message channels, send the request message on the
     * input channel and invoke the validator on the response received on the output channel.
     * This can handle subscribable or pollable output channels.
     */
    @Test
    public void testRequestResponseScenarios(){
        int i = 1;
        for (RequestResponseScenario scenario: scenarios){
            String name = scenario.getName() == null? "scenario-"+(i++) : scenario.getName();
            scenario.init();
            MessageChannel inputChannel = applicationContext.getBean(scenario.getInputChannelName(),MessageChannel.class);
            MessageChannel outputChannel = applicationContext.getBean(scenario.getOutputChannelName(),MessageChannel.class);
            if (outputChannel instanceof SubscribableChannel){
                ((SubscribableChannel) outputChannel).subscribe(scenario.getResponseValidator());
            }

            assertTrue(name + ": message not sent on " + scenario.getInputChannelName()
                    , inputChannel.send(scenario.getMessage()));

            if (outputChannel instanceof PollableChannel){
                Message<?> response = ((PollableChannel) outputChannel).receive(10000);
                assertNotNull(name + ": receive timeout on " + scenario.getOutputChannelName(),response);
                scenario.getResponseValidator().handleMessage(response);
            }

            assertNotNull("message was not handled on " + outputChannel + " for scenario '" + name + "'.",
					scenario.getResponseValidator().getLastMessage());

            if (outputChannel instanceof SubscribableChannel){
                ((SubscribableChannel) outputChannel).unsubscribe(scenario.getResponseValidator());
            }
        }
    }
    /**
     * Implement this method to define RequestResponse scenarios
     * @return - A List of {@link RequestResponseScenario}
     */
    protected abstract List<RequestResponseScenario> defineRequestResponseScenarios();


}
