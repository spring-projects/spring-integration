/**
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.smb.inbound;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbFile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.integration.Message;
import org.springframework.integration.smb.AbstractBaseTest;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;

/**
 * @author Markus Spann
 * @since 2.1.1
 */
public class SmbInboundRemoteFileSystemSynchronizerTest extends AbstractBaseTest {

	private SmbSession        smbSession;
	private SmbSessionFactory smbSessionFactory;
	private String            testLocalDir  = "test-temp/local-9/";
	private String            testRemoteDir = "test-temp/remote-9/";

	@Before
	public void prepare() {
		delete(testLocalDir);
		ensureExists(testRemoteDir);

		smbSession = mock(SmbSession.class);
		smbSessionFactory = new TestSmbSessionFactory();
		smbSessionFactory.setHost("localhost");
		smbSessionFactory.setPort(0);
		smbSessionFactory.setDomain("");
		smbSessionFactory.setUsername("sambaguest");
		smbSessionFactory.setPassword("sambaguest");
		smbSessionFactory.setShareAndDir("smb-share/");
		smbSessionFactory.setReplaceFile(true);
	}

	@Test
	public void testCopyFileToLocalDir() throws Exception {
		File localDirectoy = new File(testLocalDir);
		assertFileNotExists(localDirectoy);

		SmbInboundFileSynchronizer synchronizer = spy(new SmbInboundFileSynchronizer(smbSessionFactory));
		synchronizer.setDeleteRemoteFiles(true);
		synchronizer.setRemoteDirectory(testRemoteDir);
		synchronizer.setFilter(new SmbRegexPatternFileListFilter(".*\\.test$"));

		SmbInboundFileSynchronizingMessageSource messageSource = new SmbInboundFileSynchronizingMessageSource(synchronizer);
		messageSource.setAutoCreateLocalDirectory(true);

		messageSource.setLocalDirectory(localDirectoy);
		messageSource.afterPropertiesSet();

		String[] testFiles = new String[] {"a.test", "b.test"};

		for (String testFile : testFiles) {
			Message<File> message = messageSource.receive();
			assertNotNull(message);
			assertEquals(testFile, message.getPayload().getName());
			assertFileExists(new File(testLocalDir + "/" + testFile));
        }

		Message<File> nothing = messageSource.receive();
		assertNull(nothing);

		// two times because on the third receive (above) the internal queue will be empty
		verify(synchronizer, times(2)).synchronizeToLocalDirectory(localDirectoy);
	}

	class TestSmbSessionFactory extends SmbSessionFactory {

		@Override
		protected SmbSession createSession() {
			try {
				List<SmbFile> smbFiles = new ArrayList<SmbFile>();
				for (String fileName : new File(testRemoteDir).list()) {
					SmbFile file = smbSession.createSmbFileObject(fileName);
					smbFiles.add(file);

					doAnswer(new Answer<Object>() {
						public Object answer(InvocationOnMock _invocation) throws Throwable {
							String path = (String) _invocation.getArguments()[0];
							OutputStream os = (OutputStream) _invocation.getArguments()[1];
							writeToFile((this.getClass().getSimpleName() + " : TEST : " + path).getBytes(), os);
							return null;
						}
					}).when(smbSession).read(Mockito.eq(testRemoteDir + "/" + fileName), Mockito.any(OutputStream.class));
				}

				when(smbSession.list(testRemoteDir)).thenReturn(smbFiles.toArray(new SmbFile[] {}));
				when(smbSession.remove(Mockito.anyString())).thenReturn(true);
				return smbSession;

			} catch (Exception _ex) {
				throw new RuntimeException("Failed to create mock session.", _ex);
			}
		}
	}
}
