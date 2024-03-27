/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.store;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.store.MessageGroupStore.MessageGroupCallback;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Dave Turanski
 * @author Artem Bilan
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
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

	@Autowired
	private MessageChannel aggChannel;

	@Autowired
	private PollableChannel discards;

	@Test
	public void testExpiry() throws Exception {
		messageStore.addMessageToGroup("FOO", new GenericMessage<String>("foo"));
		assertThat(messageStore.getMessageGroup("FOO").size()).isEqualTo(1);
		// wait for expiry...
		int n = 0;
		while (n++ < 200 & messageStore.getMessageGroup("FOO").size() > 0) {
			Thread.sleep(50);
		}
		assertThat(messageStore.getMessageGroup("FOO").size()).isEqualTo(0);
		assertThat(expiryCallback.groups.size()).isEqualTo(1);
	}

	@Test
	public void testSmartLifecycle() throws Exception {
		GenericMessage<String> testMessage = new GenericMessage<String>("foo");

		messageStore2.addMessageToGroup("FOO", testMessage);
		assertThat(messageStore2.getMessageGroup("FOO").size()).isEqualTo(1);

		reaper2.setExpireOnDestroy(true);
		reaper2.setTimeout(0);

		if (!reaper2.isAutoStartup()) {
			reaper2.start();
		}

		assertThat(reaper2.isRunning()).isTrue();

		//reaper timeout is set to 0, but need to ensure positive elapsed time
		Thread.sleep(1L);

		reaper2.stop();
		assertThat(!reaper2.isRunning()).isTrue();

		assertThat(messageStore2.getMessageGroup("FOO").size()).isEqualTo(0);
		assertThat(expiryCallback2.groups.size()).isEqualTo(1);

		messageStore2.addMessageToGroup("FOO", testMessage);
		assertThat(messageStore2.getMessageGroup("FOO").size()).isEqualTo(1);
		reaper2.run();
		assertThat(messageStore2.getMessageGroup("FOO").size()).isEqualTo(1);
		reaper2.stop();
		assertThat(messageStore2.getMessageGroup("FOO").size()).isEqualTo(1);
		reaper2.start();
		reaper2.run();
		assertThat(messageStore2.getMessageGroup("FOO").size()).isEqualTo(0);
		assertThat(expiryCallback2.groups.size()).isEqualTo(2);
	}

	@Test
	public void testWithAggregator() {
		this.aggChannel.send(MessageBuilder.withPayload("foo")
				.setCorrelationId("bar")
				.setSequenceSize(2)
				.setSequenceNumber(1)
				.build());
		Message<?> discard = this.discards.receive(10000);
		assertThat(discard).isNotNull();
		assertThat(discard.getPayload()).isEqualTo("foo");
	}

	public static class ExpiryCallback implements MessageGroupCallback {

		public final List<MessageGroup> groups = new ArrayList<MessageGroup>();

		@Override
		public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
			groups.add(group);
			messageGroupStore.removeMessageGroup(group.getGroupId());
		}

	}

}
