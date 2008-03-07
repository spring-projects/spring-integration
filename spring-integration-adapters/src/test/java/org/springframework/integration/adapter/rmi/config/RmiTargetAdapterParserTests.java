/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.rmi.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.rmi.RemoteException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.rmi.RmiSourceAdapter;
import org.springframework.integration.adapter.rmi.RmiTargetAdapter;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RmiTargetAdapterParserTests {

	private final SimpleChannel testChannel = new SimpleChannel();


	@Before
	public void exportRemoteHandler() throws RemoteException {
		testChannel.setBeanName("testChannel");
		RmiSourceAdapter sourceAdapter = new RmiSourceAdapter();
		sourceAdapter.setChannel(testChannel);
		sourceAdapter.setExpectReply(false);
		sourceAdapter.afterPropertiesSet();
	}

	@Test
	public void testRmiTargetAdapter() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiTargetAdapterParserTests.xml", this.getClass());
		DefaultMessageEndpoint endpoint = (DefaultMessageEndpoint) context.getBean("adapter");
		assertNotNull(endpoint);
		assertEquals(RmiTargetAdapter.class, endpoint.getHandler().getClass());
		RmiTargetAdapter adapter = (RmiTargetAdapter) endpoint.getHandler();
		adapter.handle(new StringMessage("test"));
		assertNotNull(testChannel.receive(500));
	}

}
