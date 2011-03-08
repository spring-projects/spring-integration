/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;

import org.junit.Test;

import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class ImapMailSearchTermsTests {

	@Test
	public void validateSearchTermsWhenShouldMarkAsReadNoExistingFlags() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver();
		receiver.setShouldMarkMessagesAsRead(true);
		
		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);
		
		Method compileSearchTerms = ReflectionUtils.findMethod(receiver.getClass(), "compileSearchTerms", Flags.class);
		compileSearchTerms.setAccessible(true);
		Flags flags = new Flags();
		SearchTerm searchTerms = (SearchTerm) compileSearchTerms.invoke(receiver, flags);
		assertTrue(searchTerms instanceof NotTerm);
		NotTerm notTerm = (NotTerm) searchTerms;
		Flags siFlags = new Flags();
		siFlags.add(AbstractMailReceiver.SI_USER_FLAG);
		notTerm.getTerm().equals(siFlags);
	}
	@Test
	public void validateSearchTermsWhenShouldMarkAsReadWithExistingFlags() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver();
		receiver.setShouldMarkMessagesAsRead(true);
		
		receiver.afterPropertiesSet();
		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);
		
		Method compileSearchTerms = ReflectionUtils.findMethod(receiver.getClass(), "compileSearchTerms", Flags.class);
		compileSearchTerms.setAccessible(true);
		Flags flags = new Flags();
		flags.add(Flag.ANSWERED);
		SearchTerm searchTerms = (SearchTerm) compileSearchTerms.invoke(receiver, flags);
		assertTrue(searchTerms instanceof AndTerm);
		AndTerm andTerm = (AndTerm) searchTerms;
		SearchTerm[] terms = andTerm.getTerms();
		assertEquals(2, terms.length);
		NotTerm notTerm = (NotTerm) terms[0];
		assertTrue(((FlagTerm)notTerm.getTerm()).getFlags().contains(Flag.ANSWERED));
		notTerm = (NotTerm) terms[1];
		Flags siFlags = new Flags();
		siFlags.add(AbstractMailReceiver.SI_USER_FLAG);
		assertTrue(((FlagTerm)notTerm.getTerm()).getFlags().contains(siFlags));
	}
	
	@Test
	public void validateSearchTermsWhenShouldNotMarkAsReadNoExistingFlags() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver();
		receiver.setShouldMarkMessagesAsRead(false);
		receiver.afterPropertiesSet();
		
		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);
		
		Method compileSearchTerms = ReflectionUtils.findMethod(receiver.getClass(), "compileSearchTerms", Flags.class);
		compileSearchTerms.setAccessible(true);
		Flags flags = new Flags();
		SearchTerm searchTerms = (SearchTerm) compileSearchTerms.invoke(receiver, flags);
		assertTrue(searchTerms instanceof NotTerm);
	}
}
