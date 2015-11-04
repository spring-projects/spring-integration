/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.sftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.rules.TemporaryFolder;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.1
 *
 */
public class TestSftpServer implements InitializingBean, DisposableBean {

	private final SshServer server = SshServer.setUpDefaultServer();

	private final TemporaryFolder sftpFolder;

	private final TemporaryFolder localFolder;

	private volatile File sftpRootFolder;

	private volatile File sourceSftpDirectory;

	private volatile File targetSftpDirectory;

	private volatile File sourceLocalDirectory;

	private volatile File targetLocalDirectory;

	public TestSftpServer() {
		this.sftpFolder = new TemporaryFolder() {

			@Override
			public void create() throws IOException {
				super.create();
				sftpRootFolder = this.newFolder("test");
				sourceSftpDirectory = new File(sftpRootFolder, "sftpSource");
				sourceSftpDirectory.mkdir();
				File file = new File(sourceSftpDirectory, "sftpSource1.txt");
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file);
				fos.write("source1".getBytes());
				fos.close();
				file = new File(sourceSftpDirectory, "sftpSource2.txt");
				file.createNewFile();
				fos = new FileOutputStream(file);
				fos.write("source2".getBytes());
				fos.close();

				File subSourceFtpDirectory = new File(sourceSftpDirectory, "subSftpSource");
				subSourceFtpDirectory.mkdir();
				file = new File(subSourceFtpDirectory, "subSftpSource1.txt");
				file.createNewFile();
				fos = new FileOutputStream(file);
				fos.write("subSource1".getBytes());
				fos.close();

				targetSftpDirectory = new File(sftpRootFolder, "sftpTarget");
				targetSftpDirectory.mkdir();
			}
		};
		this.localFolder  = new TemporaryFolder() {

			@Override
			public void create() throws IOException {
				super.create();
				File rootFolder = this.newFolder("test");
				sourceLocalDirectory = new File(rootFolder, "localSource");
				sourceLocalDirectory.mkdirs();
				File file = new File(sourceLocalDirectory, "localSource1.txt");
				file.createNewFile();
				file = new File(sourceLocalDirectory, "localSource2.txt");
				file.createNewFile();

				File subSourceLocalDirectory = new File(sourceLocalDirectory, "subLocalSource");
				subSourceLocalDirectory.mkdir();
				file = new File(subSourceLocalDirectory, "subLocalSource1.txt");
				file.createNewFile();

				targetLocalDirectory = new File(rootFolder, "slocalTarget");
				targetLocalDirectory.mkdir();
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public void afterPropertiesSet() throws Exception {
		this.sftpFolder.create();
		this.localFolder.create();
		server.setPasswordAuthenticator(new PasswordAuthenticator() {

			@Override
			public boolean authenticate(String arg0, String arg1, ServerSession arg2) {
				return true;
			}

		});
		server.setPort(0);
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
		this.server.setSubsystemFactories(Collections.<NamedFactory<Command>>singletonList(new SftpSubsystem.Factory()));
		this.server.setFileSystemFactory(new VirtualFileSystemFactory(sftpRootFolder.getAbsolutePath()));
		server.start();
	}

	@Override
	public void destroy() throws Exception {
		this.server.stop(true);
		this.sftpFolder.delete();
		this.localFolder.delete();
	}

	public File getSourceLocalDirectory() {
		return this.sourceLocalDirectory;
	}

	public File getTargetLocalDirectory() {
		return this.targetLocalDirectory;
	}

	public String getTargetLocalDirectoryName() {
		return this.targetLocalDirectory.getAbsolutePath() + File.separator;
	}

	public File getTargetSftpDirectory() {
		return this.targetSftpDirectory;
	}

	public void recursiveDelete(File file) {
		File[] files = file.listFiles();
		if (files != null) {
			for (File each : files) {
				recursiveDelete(each);
			}
		}
		if (!(file.equals(this.targetSftpDirectory) || file.equals(this.targetLocalDirectory))) {
			file.delete();
		}
	}

	public DefaultSftpSessionFactory getSessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost("localhost");
		factory.setPort(this.server.getPort());
		factory.setUser("foo");
		factory.setPassword("foo");
		factory.setAllowUnknownKeys(true);
		return factory;
	}

}
