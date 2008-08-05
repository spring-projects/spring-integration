package org.springframework.integration.adapter.mail;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.internet.MimeMessage;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class SubscribableMailSourceTests {

	TaskExecutor executor;

	@Before
	public void setUp() {
		executor = new ConcurrentTaskExecutor();
	}

	@Test
	public void testReceive() throws Exception {
		javax.mail.Message message = EasyMock.createMock(MimeMessage.class);
		StubFolderConnection folderConnection = new StubFolderConnection(
				message);
		StubTarget target = new StubTarget();

		SubscribableMailSource mailSource = new SubscribableMailSource(
				folderConnection, executor);
		mailSource.subscribe(target);
		mailSource.setConverter(new StubMessageConvertor());

		mailSource.start();
		Thread.sleep(1000);
		mailSource.stop();

		assertEquals("Wrong message count", 1, target.messages.size());
		assertEquals("Wrong payload", message, target.messages.get(0)
				.getPayload());
	}

	private static class StubFolderConnection implements FolderConnection {

		ConcurrentLinkedQueue<javax.mail.Message> messages = new ConcurrentLinkedQueue<javax.mail.Message>();

		public StubFolderConnection(javax.mail.Message message) {
			messages.add(message);
		}

		public javax.mail.Message[] receive() {
			javax.mail.Message msg = messages.poll();
			if (msg == null) {
				return new javax.mail.Message[] {};
			}
			return new javax.mail.Message[] { msg };
		}

		public boolean isRunning() {
			return false;
		}

		public void start() {

		}

		public void stop() {

		}

	}

	private static class StubTarget implements MessageTarget {

		List<Message<?>> messages = new ArrayList<Message<?>>();

		public boolean send(Message<?> message) {
			messages.add(message);
			return true;
		}

	}

	private static class StubMessageConvertor implements MailMessageConverter {

		@SuppressWarnings("unchecked")
		public Message create(MimeMessage mailMessage) {
			return new GenericMessage<MimeMessage>(mailMessage);
		}
	}

}
