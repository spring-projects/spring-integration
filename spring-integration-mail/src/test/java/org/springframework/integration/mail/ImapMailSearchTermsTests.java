/*
 * Copyright 2002-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class ImapMailSearchTermsTests {

	@Test
	public void validateSearchTermsWhenShouldMarkAsReadNoExistingFlags() throws Exception {
		String userFlag = AbstractMailReceiver.DEFAULT_SI_USER_FLAG;
		ImapMailReceiver receiver = new ImapMailReceiver();
		validateSearchTermsWhenShouldMarkAsReadNoExistingFlagsGuts(userFlag, receiver);
	}

	@Test
	public void validateSearchTermsWhenShouldMarkAsReadNoExistingFlagsCustom() throws Exception {
		String userFlag = "foo";
		ImapMailReceiver receiver = new ImapMailReceiver();
		receiver.setUserFlag(userFlag);
		validateSearchTermsWhenShouldMarkAsReadNoExistingFlagsGuts(userFlag, receiver);
	}

	public void validateSearchTermsWhenShouldMarkAsReadNoExistingFlagsGuts(String userFlag, ImapMailReceiver receiver)
			throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
		receiver.setShouldMarkMessagesAsRead(true);
		receiver.setBeanFactory(mock(BeanFactory.class));

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		Method compileSearchTerms = ReflectionUtils.findMethod(receiver.getClass(), "compileSearchTerms", Flags.class);
		compileSearchTerms.setAccessible(true);
		Flags flags = new Flags();
		SearchTerm searchTerms = (SearchTerm) compileSearchTerms.invoke(receiver, flags);
		assertThat(searchTerms instanceof NotTerm).isTrue();
		NotTerm notTerm = (NotTerm) searchTerms;
		Flags siFlags = new Flags();
		siFlags.add(userFlag);
		assertThat(((FlagTerm) notTerm.getTerm()).getFlags()).isEqualTo(siFlags);
	}

	@Test
	public void validateSearchTermsWhenShouldMarkAsReadWithExistingFlags() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver();
		receiver.setShouldMarkMessagesAsRead(true);
		receiver.setBeanFactory(mock(BeanFactory.class));

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
		assertThat(searchTerms instanceof AndTerm).isTrue();
		AndTerm andTerm = (AndTerm) searchTerms;
		SearchTerm[] terms = andTerm.getTerms();
		assertThat(terms.length).isEqualTo(2);
		NotTerm notTerm = (NotTerm) terms[0];
		assertThat(((FlagTerm) notTerm.getTerm()).getFlags().contains(Flag.ANSWERED)).isTrue();
		notTerm = (NotTerm) terms[1];
		Flags siFlags = new Flags();
		siFlags.add(AbstractMailReceiver.DEFAULT_SI_USER_FLAG);
		assertThat(((FlagTerm) notTerm.getTerm()).getFlags().contains(siFlags)).isTrue();
	}

	@Test
	public void validateSearchTermsWhenShouldNotMarkAsReadNoExistingFlags() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver();
		receiver.setShouldMarkMessagesAsRead(false);
		receiver.setBeanFactory(mock(BeanFactory.class));
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
		assertThat(searchTerms instanceof NotTerm).isTrue();
	}

}
