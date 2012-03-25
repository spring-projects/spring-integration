/*
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
package org.springframework.integration.file.remote.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.session.ExtendedSession;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.message.GenericMessage;


/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@SuppressWarnings("rawtypes")
public class RemoteFileOutboundGatewayTests {

	private String tmpDir = System.getProperty("java.io.tmpdir");

	
	@Test
	public void testLs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/x/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x"));
		assertEquals(2, out.getPayload().size());
		assertSame(files[1], out.getPayload().get(0)); // sort by default
		assertSame(files[0], out.getPayload().get(1));
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test
	public void testMGetWild() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "mget", "payload");
		gw.setLocalDirectory(new File(this.tmpDir ));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		new File(this.tmpDir + "/f2").delete();
		when(sessionFactory.getSession()).thenReturn(new ExtendedSession() {
			public boolean remove(String path) throws IOException {
				return false;
			}
			public Object[] list(String path) throws IOException {
				return null;
			}
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testData".getBytes());
			}
			public void write(InputStream inputStream, String destination)
					throws IOException {
			}
			public boolean mkdir(String directory) throws IOException {
				return false;
			}
			public void rename(String pathFrom, String pathTo)
					throws IOException {
			}
			public void close() {
			}
			public boolean isOpen() {
				return false;
			}
			public boolean exists(String path) throws IOException {
				return false;
			}
			public String[] listNames(String path) throws IOException {
				return new String[] {"f1", "f2"};
			}
		});
		@SuppressWarnings("unchecked")
		Message<List<File>> out = (Message<List<File>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/*"));
		assertEquals(2, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0).getName());
		assertEquals("f2", out.getPayload().get(1).getName());
		assertEquals("testremote/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test
	public void testMGetSingle() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "mget", "payload");
		gw.setLocalDirectory(new File(this.tmpDir ));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		when(sessionFactory.getSession()).thenReturn(new ExtendedSession() {
			public boolean remove(String path) throws IOException {
				return false;
			}
			public Object[] list(String path) throws IOException {
				return null;
			}
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testData".getBytes());
			}
			public void write(InputStream inputStream, String destination)
					throws IOException {
			}
			public boolean mkdir(String directory) throws IOException {
				return false;
			}
			public void rename(String pathFrom, String pathTo)
					throws IOException {
			}
			public void close() {
			}
			public boolean isOpen() {
				return false;
			}
			public boolean exists(String path) throws IOException {
				return false;
			}
			public String[] listNames(String path) throws IOException {
				return new String[] {"f1"};
			}
		});
		@SuppressWarnings("unchecked")
		Message<List<File>> out = (Message<List<File>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/f1"));
		assertEquals(1, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0).getName());
		assertEquals("testremote/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test(expected=MessagingException.class)
	public void testMGetEmpty() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "mget", "payload");
		gw.setLocalDirectory(new File(this.tmpDir ));
		gw.setOptions("   -x   ");
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		new File(this.tmpDir + "/f2").delete();
		when(sessionFactory.getSession()).thenReturn(new ExtendedSession() {
			public boolean remove(String path) throws IOException {
				return false;
			}
			public Object[] list(String path) throws IOException {
				return null;
			}
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testData".getBytes());
			}
			public void write(InputStream inputStream, String destination)
					throws IOException {
			}
			public boolean mkdir(String directory) throws IOException {
				return false;
			}
			public void rename(String pathFrom, String pathTo)
					throws IOException {
			}
			public void close() {
			}
			public boolean isOpen() {
				return false;
			}
			public boolean exists(String path) throws IOException {
				return false;
			}
			public String[] listNames(String path) throws IOException {
				return new String[0];
			}
		});
		gw.handleRequestMessage(new GenericMessage<String>("testremote/*"));
	}

	/**
	 * @return
	 */
	public TestLsEntry[] fileList() {
		TestLsEntry[] files = new TestLsEntry[6];
		files[0] = new TestLsEntry("f2", 123, false, false, 1234, "-r--r--r--");
		files[1] = new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--");
		files[2] = new TestLsEntry("f3", 12345, true, false, 123456, "drw-r--r--");
		files[3] = new TestLsEntry("f4", 12346, false, true, 1234567, "lrw-r--r--");
		files[4] = new TestLsEntry(".f5", 12347, false, false, 12345678, "-rw-r--r--");
		files[5] = new TestLsEntry(".f6", 12347, true, false, 123456789, "drw-r--r--");
		return files;
	}

	@Test
	public void testLs_f() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		gw.setOptions("-f");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/x/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x"));
		assertEquals(2, out.getPayload().size());
		assertSame(files[0], out.getPayload().get(0));
		assertSame(files[1], out.getPayload().get(1));
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test
	public void testLs_None() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = new TestLsEntry[0];
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(0, out.getPayload().size());
	}

	@Test
	public void testLs_1() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		gw.setOptions("-1");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(2, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0));
		assertEquals("f2", out.getPayload().get(1));
	}

	@Test
	public void testLs_1_f() throws Exception { //no sort
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -f");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(2, out.getPayload().size());
		assertEquals("f2", out.getPayload().get(0));
		assertEquals("f1", out.getPayload().get(1));
	}

	@Test
	public void testLs_1_dirs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -dirs");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(3, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0));
		assertEquals("f2", out.getPayload().get(1));
		assertEquals("f3", out.getPayload().get(2));
	}

	@Test
	public void testLs_1_dirs_links() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -dirs -links");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(4, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0));
		assertEquals("f2", out.getPayload().get(1));
		assertEquals("f3", out.getPayload().get(2));
		assertEquals("f4", out.getPayload().get(3));
	}

	@Test
	public void testLs_1_a_f_dirs_links() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -a -f -dirs -links");
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(6, out.getPayload().size());
		assertEquals("f2", out.getPayload().get(0));
		assertEquals("f1", out.getPayload().get(1));
		assertEquals("f3", out.getPayload().get(2));
		assertEquals("f4", out.getPayload().get(3));
		assertEquals(".f5", out.getPayload().get(4));
		assertEquals(".f6", out.getPayload().get(5));
	}

	@Test
	public void testLs_1_a_f_dirs_links_filtered() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -a -f -dirs -links");
		gw.setFilter(new TestPatternFilter("*4"));
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(1, out.getPayload().size());
		assertEquals("f4", out.getPayload().get(0));
	}

	@Test
	public void testGet() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir ));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		when(sessionFactory.getSession()).thenReturn(new Session(){
			private boolean open = true;
			public boolean remove(String path) throws IOException {
				return false;
			}
	
			public TestLsEntry[] list(String path) throws IOException {
				return new TestLsEntry[] {
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testfile".getBytes());
			}
			public void write(InputStream inputStream, String destination)
					throws IOException {
			}
			public boolean mkdir(String directory) throws IOException {
				return true;
			}
			public void rename(String pathFrom, String pathTo)
					throws IOException {
			}
			public void close() {
				open = false;
			}
			public boolean isOpen() {
				return open;
			}

			public boolean exists(String path) throws IOException {
				return true;
			}
		});
		@SuppressWarnings("unchecked")
		Message<File> out = (Message<File>) gw.handleRequestMessage(new GenericMessage<String>("f1"));
		File outFile = new File(this.tmpDir + "/f1");
		assertEquals(outFile, out.getPayload());
		assertTrue(outFile.exists());
		outFile.delete();
		assertEquals("/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("f1",
				out.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testGet_P() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.setOptions("-P");
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		final Date modified = new Date(cal.getTime().getTime() / 1000 * 1000);
		when(sessionFactory.getSession()).thenReturn(new Session(){
			private boolean open = true;
			public boolean remove(String path) throws IOException {
				return false;
			}
			
			public TestLsEntry[] list(String path) throws IOException {
				return new TestLsEntry[] {
						new TestLsEntry("f1", 1234, false, false, modified.getTime(), "-rw-r--r--")
				};
			}
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testfile".getBytes());
			}
			public void write(InputStream inputStream, String destination)
					throws IOException {
			}
			public boolean mkdir(String directory) throws IOException {
				return true;
			}
			public void rename(String pathFrom, String pathTo)
					throws IOException {
			}
			public void close() {
				open = false;
			}
			public boolean isOpen() {
				return open;
			} 
			public boolean exists(String path) throws IOException {
				return true;
			}
		});
		@SuppressWarnings("unchecked")
		Message<File> out = (Message<File>) gw.handleRequestMessage(new GenericMessage<String>("x/f1"));
		File outFile = new File(this.tmpDir + "/f1");
		assertEquals(outFile, out.getPayload());
		assertTrue(outFile.exists());
		assertEquals(modified.getTime(), outFile.lastModified());
		outFile.delete();
		assertEquals("x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("f1",
				out.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testGet_create_dir() throws Exception {
		new File(this.tmpDir + "/x/f1").delete();
		new File(this.tmpDir + "/x").delete();
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir + "/x"));
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(new Session(){
			private boolean open = true;
			public boolean remove(String path) throws IOException {
				return false;
			}
			
			public TestLsEntry[] list(String path) throws IOException {
				return new TestLsEntry[] {
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testfile".getBytes());
			}
			public void write(InputStream inputStream, String destination)
					throws IOException {
			}
			public boolean mkdir(String directory) throws IOException {
				return true;
			}
			public void rename(String pathFrom, String pathTo)
					throws IOException {
			}
			public void close() {
				open = false;
			}
			public boolean isOpen() {
				return open;
			} 
			public boolean exists(String path) throws IOException {
				return true;
			}
		});
		gw.handleRequestMessage(new GenericMessage<String>("f1"));
		File out = new File(this.tmpDir + "/x/f1");
		assertTrue(out.exists());
		out.delete();
	}

	@Test
	public void testRm() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
			(sessionFactory, "rm", "payload");
		when(sessionFactory.getSession()).thenReturn(session);
		when(session.remove("testremote/x/f1")).thenReturn(Boolean.TRUE);
		@SuppressWarnings("unchecked")
		Message<Boolean> out = (Message<Boolean>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x/f1"));
		assertEquals(Boolean.TRUE, out.getPayload());
		verify(session).remove("testremote/x/f1");
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("f1",
				out.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

}

class TestRemoteFileOutboundGateway extends AbstractRemoteFileOutboundGateway<TestLsEntry> {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TestRemoteFileOutboundGateway(SessionFactory sessionFactory,
			String command, String expression) {
		super(sessionFactory, command, expression);
	}

	@Override
	protected boolean isDirectory(TestLsEntry file) {
		return file.isDirectory();
	}

	@Override
	protected boolean isLink(TestLsEntry file) {
		return file.isLink();
	}

	@Override
	protected String getFilename(TestLsEntry file) {
		return file.getFilename();
	}

	@Override
	protected long getModified(TestLsEntry file) {
		return file.getModified();
	}

	@Override
	protected List<AbstractFileInfo<TestLsEntry>> asFileInfoList(
			Collection<TestLsEntry> files) {
		return new ArrayList<AbstractFileInfo<TestLsEntry>>(files);
	}

}

class TestLsEntry extends AbstractFileInfo<TestLsEntry> {

	private String filename;
	private int size;
	private boolean dir;
	private boolean link;
	private long modified;
	private String permissions;

	public TestLsEntry(String filename, int size, boolean dir, boolean link,
			long modified, String permissions) {
		this.filename = filename;
		this.size = size;
		this.dir = dir;
		this.link = link;
		this.modified = modified;
		this.permissions = permissions;
	}

	public boolean isDirectory() {
		return this.dir;
	}

	public long getModified() {
		return this.modified;
	}

	public String getFilename() {
		return this.filename;
	}

	public boolean isLink() {
		return this.link;
	}

	public long getSize() {
		return this.size;
	}

	public String getPermissions() {
		return this.permissions;
	}

	public TestLsEntry getFileInfo() {
		return this;
	}

}

class TestPatternFilter extends AbstractSimplePatternFileListFilter<TestLsEntry>{

	public TestPatternFilter(String path) {
		super(path);
	}

	@Override
	protected String getFilename(TestLsEntry file) {
		return file.getFilename();
	}

}