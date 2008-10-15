/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.ftp;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.reset;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.oro.io.Perl5FilenameFilter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.core.Message;

/**
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class FtpSourceTests {

	private FTPClient ftpClient = createMock(FTPClient.class);

	private FTPFile ftpFile = createMock(FTPFile.class);

	private FTPClientPool ftpClientPool = createNiceMock(FTPClientPool.class);

	private Object[] globalMocks = new Object[] { ftpClient, ftpFile, ftpClientPool };

	private FtpSource ftpSource;

	private Long size = 100l;


	@Before
	public void liberalPool() throws Exception {
		expect(ftpClientPool.getClient()).andReturn(ftpClient).anyTimes();
	}

	@Before
	public void initializeFtpSource() {
		ftpSource = new FtpSource(ftpClientPool);
	}

	@Before
	public void clearState() {
		reset(globalMocks);
	}


	@Test
	public void retrieveSingleFile() throws Exception {
		expect(ftpClient.listFiles()).andReturn(mockedFTPFilesNamed("test1"));
		expect(ftpClient.retrieveFile(eq("test1"), isA(OutputStream.class))).andReturn(true);
		replay(globalMocks);
		Message<List<File>> received = ftpSource.receive();
		ftpSource.onSend(received);
		verify(globalMocks);
	}

	private FTPFile[] mockedFTPFilesNamed(String... names) {
		List<FTPFile> files = new ArrayList<FTPFile>();
		// ensure difference by increasing size
		Calendar timestamp = Calendar.getInstance();
		size++;
		for (String name : names) {
			FTPFile ftpFile = createMock(FTPFile.class);
			expect(ftpFile.getName()).andReturn(name).anyTimes();
			expect(ftpFile.getTimestamp()).andReturn(timestamp).anyTimes();
			expect(ftpFile.getSize()).andReturn(size).anyTimes();
			files.add(ftpFile);
			replay(ftpFile);
		}
		return files.toArray(new FTPFile[] {});
	}

	@Test
	public void retrieveMultipleFiles() throws Exception {
		// get files
		expect(ftpClient.listFiles()).andReturn(mockedFTPFilesNamed("test1", "test2")).times(2);
		expect(ftpClient.retrieveFile(eq("test1"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);
		List<File> files = Arrays.asList(new File("test1"), new File("test2"));

		replay(globalMocks);
		Message receivedFiles = ftpSource.receive();
		ftpSource.onSend(receivedFiles);
		Message<List<File>> secondReceived = ftpSource.receive();
		verify(globalMocks);
		assertEquals(files, receivedFiles.getPayload());
		assertNull(secondReceived);
	}

	@Test
	public void retrieveMultipleChangingFiles() throws Exception {
		// first run
		FTPFile[] mockedFTPFiles = mockedFTPFilesNamed("test1", "test2");
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles);
		expect(ftpClient.retrieveFile(eq("test1"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);

		// second run, change the date so the messages should be retrieved again
		FTPFile[] mockedFTPFiles2 = mockedFTPFilesNamed("test1", "test2");
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles2);
		expect(ftpClient.retrieveFile(eq("test1"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);
		List<File> files = Arrays.asList(new File("test1"), new File("test2"));

		replay(globalMocks);
		Message receivedFiles = ftpSource.receive();
		ftpSource.onSend(receivedFiles);
		ftpSource.onSend(ftpSource.receive());
		verify(globalMocks);
		assertEquals(files, receivedFiles.getPayload());
	}

	@Test
	public void retrieveMaxFilesPerMessage() throws Exception {

		this.ftpSource.setMaxFilesPerMessage(2);
		// assume client already connected
		FTPFile[] mockedFTPFiles = mockedFTPFilesNamed("test1", "test2", "test3");

		// expect two receive runs
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles).times(2);

		// first run
		expect(ftpClient.retrieveFile(eq("test1"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);
		// second run
		expect(ftpClient.retrieveFile(eq("test3"), isA(OutputStream.class))).andReturn(true);

		replay(globalMocks);
		Message<List<File>> receivedFiles1 = ftpSource.receive();
		ftpSource.onSend(receivedFiles1);
		Message<List<File>> receivedFiles2 = ftpSource.receive();
		ftpSource.onSend(receivedFiles2);
		verify(globalMocks);
		List<File> allReceived = new ArrayList<File>(receivedFiles1.getPayload());
		allReceived.addAll(receivedFiles2.getPayload());
		assertEquals(2, receivedFiles1.getPayload().size());
		assertEquals(1, receivedFiles2.getPayload().size());
		assertTrue(allReceived.containsAll(Arrays.asList(new File[] { new File("test1"), new File("test2"),
				new File("test3") })));
	}

	@Test(timeout = 6000)
	@Ignore //not reliable
	public void concurrentPollingSunnyDay() throws Exception {
		final CountDownLatch recorded = new CountDownLatch(1);
		this.ftpSource.setMaxFilesPerMessage(2);
		// first run
		FTPFile[] mockedFTPFiles = mockedFTPFilesNamed("test1", "test2", "test3", "test4", "test5");
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles);
		expect(ftpClient.retrieveFile(eq("test1"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);

		// second poll
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles);
		expect(ftpClient.retrieveFile(eq("test3"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test4"), isA(OutputStream.class))).andReturn(true);

		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles);
		expect(ftpClient.retrieveFile(eq("test5"), isA(OutputStream.class))).andReturn(true);
		replay(globalMocks);
		recorded.countDown();

		final CountDownLatch receivesDone = new CountDownLatch(3);

		for (int i = 0; i < 3; i++) {
			new Thread(new Runnable() {
				public void run() {
					Message<List<File>> recievedFiles = null;
					try {
						// make sure receive happens after recording
						recorded.await();
						recievedFiles = ftpSource.receive();
						receivesDone.countDown();
						// make sure onSend happens after all receives
						receivesDone.await();
					}
					catch (InterruptedException e) {
					}
					finally {
						ftpSource.onSend(recievedFiles);
					}
				}
			}).start();
		}
		try {
			receivesDone.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		verify(globalMocks);
	}

	@Test
	public void onFailure() throws Exception {
		expect(ftpClient.listFiles()).andReturn(mockedFTPFilesNamed("test1")).times(2);
		expect(ftpClient.retrieveFile(eq("test1"), isA(OutputStream.class))).andReturn(true).times(2);
		replay(globalMocks);
		Message<List<File>> received = ftpSource.receive();
		ftpSource.onFailure(received, new Exception("just a test"));
		assertEquals(received.getPayload(), ftpSource.receive().getPayload());
		verify(globalMocks);
	}

	@AfterClass
	public static void deleteFiles() {
		File file = new File("./");
		File[] files = file.listFiles((FilenameFilter) new Perl5FilenameFilter("test\\d"));
		for (File file2 : files) {
			file2.delete();
		}
	}

}
