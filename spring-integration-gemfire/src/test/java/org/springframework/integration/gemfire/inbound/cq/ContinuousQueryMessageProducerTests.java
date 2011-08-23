/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.gemfire.inbound.cq;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.gemfire.fork.ForkUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.query.CqEvent;
import com.gemstone.gemfire.internal.cache.LocalRegion;

/**
 * @author David Turanski
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ContinuousQueryMessageProducerTests {
	
	@Autowired
	LocalRegion region;
	
	@Autowired
	ConfigurableApplicationContext applicationContext;
	
	@Autowired 
	PollableChannel outputChannel;
	
	static OutputStream os;
	@BeforeClass
	public static void startUp() throws Exception {
		os = ForkUtil.cacheServer();
	}
	
	 
	
	@Test
	public void test()  throws InterruptedException {
		region.put("one",1);
		Message<?> msg = outputChannel.receive(1000);
		assertNotNull(msg);
		assertTrue(msg.getPayload() instanceof CqEvent);
		/*
		 * Avoid shutdown errors
		 */
		applicationContext.close();
	}
	
	@AfterClass
	public static void cleanUp()  { 
		
		try {
			Thread.sleep(3000);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		sendSignal();
	}
	
	public static void sendSignal() {
		try {
			os.write("\n".getBytes());
			os.flush();
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot communicate with forked VM", ex);
		}
	}
}
