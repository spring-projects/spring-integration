/*
 * Copyright 2009-2010 the original author or authors.
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
package org.springframework.integration.monitor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChannelIntegrationTests {

	@Autowired
	private MessageChannel requests;

	@Autowired
	private PollableChannel intermediate;

	@Autowired
	private IntegrationMBeanExporter messageChannelsMonitor;

	@Test
	public void testMessageChannelStatistics() throws Exception {
		
		requests.send(new GenericMessage<String>("foo"));

		double rate = messageChannelsMonitor.getChannelSendRate("" + requests).getMean();
		assertTrue("No statistics for requests channel", rate >= 0);

		rate = messageChannelsMonitor.getChannelSendRate("" + intermediate).getMean();
		assertTrue("No statistics for intermediate channel", rate >= 0);
		
		assertNotNull(intermediate.receive(100L));
		double count = messageChannelsMonitor.getChannelReceiveCount("" + intermediate);
		assertTrue("No statistics for intermediate channel", count >= 0);
		
	}

}
