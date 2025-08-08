/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mail;

import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0.6
 *
 */
public class MailReceiverTests {

	@Test
	public void testStoreConnectAndFolderCloseWhenNoMessages() throws Exception {
		AbstractMailReceiver receiver = new AbstractMailReceiver() {

			@Override
			protected Message[] searchForNewMessages() {
				return new Message[0];
			}

		};
		Properties props = new Properties();
		Session session = Session.getInstance(props);
		receiver.setSession(session);
		receiver.setProtocol("imap");
		receiver.setAutoCloseFolder(false);
		Store store = session.getStore("imap");
		store = spy(store);
		new DirectFieldAccessor(receiver).setPropertyValue("store", store);
		when(store.isConnected()).thenReturn(false);
		Folder folder = mock(Folder.class);
		when(folder.exists()).thenReturn(true);
		when(folder.isOpen()).thenReturn(false, true);
		doReturn(folder).when(store).getDefaultFolder();
		doNothing().when(store).connect();
		receiver.openFolder();
		receiver.openFolder();
		verify(store, times(2)).connect();

		Object[] receive = receiver.receive();
		assertThat(receive).isEmpty();

		verify(folder).close(anyBoolean());
	}

}
