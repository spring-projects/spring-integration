package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcMessageStoreChannelTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private JdbcMessageStore messageStore;
	
	@Before
	public void clear() {
		for (MessageGroup group : messageStore) {
			messageStore.removeMessageGroup(group.getGroupId());
		}
	}

	@Test
	public void testSendAndActivate() throws Exception {
		Service.reset(1);
		input.send(new StringMessage("foo"));
		Service.await(1000);
		assertEquals(1, Service.messages.size());
		assertEquals(0, messageStore.getMessageGroup("input-queue").size());
	}
	
	@Test
	public void testSendAndActivateWithRollback() throws Exception {
		Service.reset(1);
		Service.fail = true;
		input.send(new StringMessage("foo"));
		Service.await(1000);
		assertEquals(1, Service.messages.size());
		// After a rollback in the poller the message is still waiting to be delivered
		assertEquals(1, messageStore.getMessageGroup("input-queue").size());
		assertEquals(1, messageStore.getMessageGroup("input-queue").getUnmarked().size());
	}
	
	@Test
	@Transactional
	public void testSendAndActivateTransactionalSend() throws Exception {
		Service.reset(1);
		input.send(new StringMessage("foo"));
		// This will time out because the transaction has not committed yet
		Service.await(1000);
		// So no activation
		assertEquals(0, Service.messages.size());
		// But inside the transaction the message is still there
		assertEquals(1, messageStore.getMessageGroup("input-queue").size());
		assertEquals(1, messageStore.getMessageGroup("input-queue").getUnmarked().size());
	}
	
	public static class Service {
		private static boolean fail = false;
		private static List<String> messages = new CopyOnWriteArrayList<String>();
		private static CountDownLatch latch = new CountDownLatch(0);
		public static void reset(int count) {
			fail = false;
			messages.clear();
			latch = new CountDownLatch(count);
		}
		public static void await(long timeout) throws InterruptedException {
			latch.await(timeout, TimeUnit.MILLISECONDS);
		}
		public String echo(String input) {
			messages.add(input);
			latch.countDown();
			if (fail) {
				throw new RuntimeException("Planned failure");
			}
			return input;
		}
	}

}
