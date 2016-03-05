/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AutoStartTests {

	@Autowired
	AbstractServerConnectionFactory cfS1;

	@Autowired
	TcpReceivingChannelAdapter tcpNetIn;

	@Test
	public void testNetIn() throws Exception {
		assertFalse(cfS1.isAutoStartup());
		DirectFieldAccessor dfa = new DirectFieldAccessor(cfS1);
		assertNull(dfa.getPropertyValue("serverSocket"));
		startAndStop();
		assertNull(dfa.getPropertyValue("serverSocket"));
		startAndStop();
		assertNull(dfa.getPropertyValue("serverSocket"));
	}

	/**
	 * @throws InterruptedException
	 */
	private void startAndStop() throws InterruptedException {
		tcpNetIn.start();
		TestingUtilities.waitListening(cfS1, null);
		tcpNetIn.stop();
		TestingUtilities.waitStopListening(cfS1, null);
	}
}
