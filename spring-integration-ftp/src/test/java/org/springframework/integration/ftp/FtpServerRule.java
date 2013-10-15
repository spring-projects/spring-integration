/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ftp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

import org.springframework.integration.test.util.SocketUtils;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class FtpServerRule extends ExternalResource {

	public static int FTP_PORT = SocketUtils.findAvailableServerSocket();

	private final TemporaryFolder ftpFolder;

	private final TemporaryFolder localFolder;

	private volatile File ftpRootFolder;

	private volatile File sourceFtpDirectory;

	private volatile File targetFtpDirectory;

	private volatile File sourceLocalDirectory;

	private volatile File targetLocalDirectory;

	private volatile FtpServer server;

	public FtpServerRule(final String root) {
		this.ftpFolder = new TemporaryFolder() {

			@Override
			public void create() throws IOException {
				super.create();
				ftpRootFolder = this.newFolder(root);
				sourceFtpDirectory = new File(ftpRootFolder, "ftpSource");
				sourceFtpDirectory.mkdir();
				File file = new File(sourceFtpDirectory, "ftpSource1.txt");
				file.createNewFile();
				file = new File(sourceFtpDirectory, "ftpSource2.txt");
				file.createNewFile();

				File subSourceFtpDirectory = new File(sourceFtpDirectory, "subFtpSource");
				subSourceFtpDirectory.mkdir();
				file = new File(subSourceFtpDirectory, "subFtpSource1.txt");
				file.createNewFile();

				targetFtpDirectory = new File(ftpRootFolder, "ftpTarget");
				targetFtpDirectory.mkdirs();
			}
		};
		this.localFolder  = new TemporaryFolder() {

			@Override
			public void create() throws IOException {
				super.create();
				File rootFolder = this.newFolder(root);
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

				targetLocalDirectory = new File(rootFolder, "localTarget");
				targetLocalDirectory.mkdirs();
			}
		};
	}

	public File getSourceFtpDirectory() {
		return sourceFtpDirectory;
	}

	public File getTargetFtpDirectory() {
		return targetFtpDirectory;
	}

	public File getSourceLocalDirectory() {
		return sourceLocalDirectory;
	}

	public File getTargetLocalDirectory() {
		return targetLocalDirectory;
	}

	@Override
	protected void before() throws Throwable {
		this.ftpFolder.create();
		this.localFolder.create();

		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new TestUserManager(this.ftpRootFolder.getAbsolutePath()));

		ListenerFactory factory = new ListenerFactory();
		factory.setPort(FTP_PORT);
		serverFactory.addListener("default", factory.createListener());

		server = serverFactory.createServer();
		server.start();
	}


	@Override
	protected void after() {
		this.server.stop();
		this.ftpFolder.delete();
		this.localFolder.delete();
	}


	public static void recursiveDelete(File file) {
		File[] files = file.listFiles();
		if (files != null) {
			for (File each : files) {
				recursiveDelete(each);
			}
		}
		file.delete();
	}


	private class TestUserManager implements UserManager {

		private final BaseUser testUser;

		private TestUserManager(String homeDirectory) {
			this.testUser = new BaseUser();
			this.testUser.setAuthorities(Arrays.asList(new ConcurrentLoginPermission(1024, 1024),
					new WritePermission(),
					new TransferRatePermission(1024, 1024)));
			this.testUser.setHomeDirectory(homeDirectory);
			this.testUser.setName("TEST_USER");
		}


		@Override
		public User getUserByName(String s) throws FtpException {
			return this.testUser;
		}

		@Override
		public String[] getAllUserNames() throws FtpException {
			return new String[]{"TEST_USER"};
		}

		@Override
		public void delete(String s) throws FtpException {
		}

		@Override
		public void save(User user) throws FtpException {
		}

		@Override
		public boolean doesExist(String s) throws FtpException {
			return true;
		}

		@Override
		public User authenticate(Authentication authentication) throws AuthenticationFailedException {
			return this.testUser;
		}

		@Override
		public String getAdminName() throws FtpException {
			return "admin";
		}

		@Override
		public boolean isAdmin(String s) throws FtpException {
			return s.equals("admin");
		}

	}

}
