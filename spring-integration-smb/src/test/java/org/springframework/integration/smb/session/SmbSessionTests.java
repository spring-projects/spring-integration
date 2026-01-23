/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.integration.smb.session;

import java.io.IOException;

import org.codelibs.jcifs.smb.impl.SmbFile;
import org.junit.jupiter.api.Test;

import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.smb.SmbTestSupport;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 *
 * @author Gunnar Hillert
 * @author Gregory Bragg
 * @author Artem Bilan
 * @author Daniel Frey
 * @author Glenn Renfro
 *
 */
public class SmbSessionTests extends SmbTestSupport {

	@Test
	public void testCreateSmbFileObjectWithBackSlash1() throws IOException {
		System.setProperty("file.separator", "\\");
		try (SmbSession smbSession = smbSessionFactory.getSession()) {
			SmbFile smbFile = smbSession.createSmbFileObject("smb://localhost\\blubba\\");
			assertThat("smb://localhost/blubba/").isEqualTo(smbFile.getPath());
		}
	}

	@Test
	public void testCreateOtherSmbFileObject() throws IOException {
		System.setProperty("file.separator", "\\");
		try (SmbSession smbSession = smbSessionFactory.getSession()) {
			SmbFile smbFile = smbSession.createSmbFileObject("..\\another");
			assertThat(smbServerUrl() + "/another").isEqualTo(smbFile.getPath());
		}
	}

	@Test
	public void testCreateSmbFileObjectWithBackSlash4() throws IOException {
		System.setProperty("file.separator", "/");
		try (SmbSession smbSession = smbSessionFactory.getSession()) {
			SmbFile smbFile = smbSession.createSmbFileObject("smb://localhost\\blubba\\");
			assertThat("smb://localhost/blubba/").isEqualTo(smbFile.getPath());
		}
	}

	@Test
	public void testCreateSmbFileObjectWithMissingTrailingSlash1() throws IOException {
		try (SmbSession smbSession = smbSessionFactory.getSession()) {
			SmbFile smbFile = smbSession.createSmbFileObject("smb://localhost\\blubba");
			assertThat("smb://localhost/blubba").isEqualTo(smbFile.getPath());
		}
	}

	@Test
	public void testCreateSmbFileObjectWithMissingTrailingSlash2() throws IOException {
		try (SmbSession smbSession = smbSessionFactory.getSession()) {
			SmbFile smbFile = smbSession.createSmbFileObject(".");
			assertThat(smbServerUrl() + '/' + SHARE_AND_DIR + '/').isEqualTo(smbFile.getPath());
		}
	}

	@Test
	public void testCreateSmbFileObjectWithMissingTrailingSlash3() throws IOException {
		try (SmbSession smbSession = smbSessionFactory.getSession()) {
			SmbFile smbFile = smbSession.createSmbFileObject("..\\anotherShare");
			assertThat(smbServerUrl() + "/anotherShare").isEqualTo(smbFile.getPath());
		}
	}

	@Test
	public void sessionIsNotDirtyOnNoSuchFileError() {
		CachingSessionFactory<SmbFile> cachingSessionFactory = new CachingSessionFactory<>(smbSessionFactory);
		Session<SmbFile> session = cachingSessionFactory.getSession();
		session.close();

		SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(cachingSessionFactory);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> template.rename("No_such_file1", "No_such_file2"))
				.withRootCauseInstanceOf(IOException.class)
				.withStackTraceContaining("The system cannot find the file specified");

		Session<SmbFile> newSession = cachingSessionFactory.getSession();
		assertThat(TestUtils.<Object>getPropertyValue(newSession, "targetSession"))
				.isSameAs(TestUtils.getPropertyValue(session, "targetSession"));

		newSession.close();
	}

}
