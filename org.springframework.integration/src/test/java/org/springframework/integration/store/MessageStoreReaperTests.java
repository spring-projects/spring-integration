/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.store;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Dave Syer
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessageStoreReaperTests {
	
	@Autowired
	private MessageGroupStore messageStore;
	
	@Before
	public void init() {
		ExpiryCallback.groups.clear();
	}

	@Test
	public void testExpiry() throws Exception {
		messageStore.addMessageToGroup("FOO", new StringMessage("foo"));
		assertEquals(1, messageStore.getMessageGroup("FOO").size());
		// wait for expiry...
		Thread.sleep(200L);
		assertEquals(0, messageStore.getMessageGroup("FOO").size());
		assertEquals(1, ExpiryCallback.groups.size());
	}
	
	public static class ExpiryCallback implements MessageGroupCallback {

		private static final List<MessageGroup> groups = new ArrayList<MessageGroup>();

		public void execute(MessageGroup group) {
			groups.add(group);
		}
		
	}

}
