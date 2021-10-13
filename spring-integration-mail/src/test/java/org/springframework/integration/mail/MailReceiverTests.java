/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.mail;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0.6
 *
 */
public class MailReceiverTests {

	@Test
	public void testStoreConnect() throws Exception {
		AbstractMailReceiver receiver = new AbstractMailReceiver() {

			@Override
			protected Message[] searchForNewMessages() {
				return null;
			}

		};
		Properties props = new Properties();
		Session session = Session.getInstance(props);
		receiver.setSession(session);
		receiver.setProtocol("imap");
		Store store = session.getStore("imap");
		store = spy(store);
		new DirectFieldAccessor(receiver).setPropertyValue("store", store);
		when(store.isConnected()).thenReturn(false);
		Folder folder = mock(Folder.class);
		when(folder.exists()).thenReturn(true);
		when(folder.isOpen()).thenReturn(false);
		doReturn(folder).when(store).getFolder((URLName) null);
		doNothing().when(store).connect();
		receiver.openFolder();
		receiver.openFolder();
		verify(store, times(2)).connect();
	}

}
