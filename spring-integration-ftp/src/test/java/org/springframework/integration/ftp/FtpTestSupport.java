/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.ftp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.server.ApacheMinaFtplet;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

/**
 * Provides an embedded FTP Server for test cases.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author David Turanski
 *
 * @since 4.3
 */
public class FtpTestSupport extends RemoteFileTestSupport {

	private static final ApacheMinaFtplet FTPLET = new ApacheMinaFtplet();

	private static volatile FtpServer server;

	@BeforeAll
	public static void createServer() throws Exception {
		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new TestUserManager(getRemoteTempFolder().getAbsolutePath()));
		ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
		connectionConfigFactory.setMaxLogins(1024);
		connectionConfigFactory.setMaxAnonymousLogins(1024);
		serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());
		ListenerFactory factory = new ListenerFactory();
		factory.setPort(0);
		serverFactory.addListener("default", factory.createListener());
		serverFactory.setFtplets(new HashMap<>(Collections.singletonMap("springFtplet", FTPLET)));
		FTPLET.setApplicationEventPublisher(ev -> {
			// no-op
		});
		server = serverFactory.createServer();
		server.start();

		Listener listener = serverFactory.getListeners().values().iterator().next();
		port = listener.getPort();
	}


	@AfterAll
	public static void stopServer() {
		server.stop();
	}

	@Override
	protected String prefix() {
		return "ftp";
	}

	public static SessionFactory<FTPFile> sessionFactory() {
		return new CachingSessionFactory<>(rawSessionFactory());
	}

	protected static DefaultFtpSessionFactory rawSessionFactory() {
		DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
		sf.setHost("localhost");
		sf.setPort(port);
		sf.setUsername("foo");
		sf.setPassword("foo");
		sf.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
		return sf;
	}

	protected static ApacheMinaFtplet ftplet() {
		return FTPLET;
	}

	private static class TestUserManager implements UserManager {

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
		public User getUserByName(String s) {
			return this.testUser;
		}

		@Override
		public String[] getAllUserNames() {
			return new String[]{ "TEST_USER" };
		}

		@Override
		public void delete(String s) throws FtpException {
		}

		@Override
		public void save(User user) {
		}

		@Override
		public boolean doesExist(String s) {
			return true;
		}

		@Override
		public User authenticate(Authentication authentication) {
			return this.testUser;
		}

		@Override
		public String getAdminName() {
			return "admin";
		}

		@Override
		public boolean isAdmin(String s) {
			return s.equals("admin");
		}

	}

}
