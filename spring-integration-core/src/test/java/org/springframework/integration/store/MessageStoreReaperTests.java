/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Dave Turanski
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessageStoreReaperTests {

	@Autowired
	@Qualifier("messageStore")
	private MessageGroupStore messageStore;

	@Autowired
	@Qualifier("messageStore2")
	private MessageGroupStore messageStore2;

	@Autowired
	@Qualifier("reaper2")
	private MessageGroupStoreReaper reaper2;

	@Autowired
	@Qualifier("expiryCallback")
	private ExpiryCallback expiryCallback;

	@Autowired
	@Qualifier("expiryCallback2")
	private ExpiryCallback expiryCallback2;

	@Test
	public void testExpiry() throws Exception {
		messageStore.addMessageToGroup("FOO", new GenericMessage<String>("foo"));
		assertEquals(1, messageStore.getMessageGroup("FOO").size());
		// wait for expiry...
		Thread.sleep(200L);
		assertEquals(0, messageStore.getMessageGroup("FOO").size());
		assertEquals(1, expiryCallback.groups.size());
	}

	@Test
	public void testSmartLifecycle() throws Exception{
		GenericMessage<String> testMessage = new GenericMessage<String>("foo");

		messageStore2.addMessageToGroup("FOO", testMessage);
		assertEquals(1, messageStore2.getMessageGroup("FOO").size());

		reaper2.setExpireOnDestroy(true);
		reaper2.setTimeout(0);


		if (!reaper2.isAutoStartup()){
			reaper2.start();
		}

		assertTrue(reaper2.isRunning());

		//reaper timeout is set to 0, but need to ensure positive elapsed time
		Thread.sleep(1L);

		reaper2.stop();
		assertTrue(!reaper2.isRunning());

		assertEquals(0, messageStore2.getMessageGroup("FOO").size());
		assertEquals(1, expiryCallback2.groups.size());

		messageStore2.addMessageToGroup("FOO", testMessage);
		assertEquals(1, messageStore2.getMessageGroup("FOO").size());
		reaper2.run();
		assertEquals(1, messageStore2.getMessageGroup("FOO").size());
		reaper2.destroy();
		assertEquals(1, messageStore2.getMessageGroup("FOO").size());
		reaper2.start();
		reaper2.run();
		assertEquals(0, messageStore2.getMessageGroup("FOO").size());
		assertEquals(2, expiryCallback2.groups.size());
	}

	public static class ExpiryCallback implements MessageGroupCallback {

		public final List<MessageGroup> groups = new ArrayList<MessageGroup>();

		public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
			groups.add(group);
			messageGroupStore.removeMessageGroup(group.getGroupId());
		}

	}

}
