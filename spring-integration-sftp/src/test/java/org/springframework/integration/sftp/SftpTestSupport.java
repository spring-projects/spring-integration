/*
 * Copyright 2016 the original author or authors.
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
import java.util.Collections;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * Provides an embedded SFTP Server for test cases.
 *
 * @author David Turanski
 * @author Gary Russell
 * @since 4.3
 */
public class SftpTestSupport extends RemoteFileTestSupport {

	private static SshServer server;

	public String getTargetLocalDirectoryName() {
		return targetLocalDirectory.getAbsolutePath() + File.separator;
	}

	@Override
	public String prefix() {
		return "sftp";
	}

	@BeforeClass
	public static void createServer() throws Exception {
		server = SshServer.setUpDefaultServer();
		server.setPasswordAuthenticator(new PasswordAuthenticator() {

			@Override
			public boolean authenticate(String username, String password,
					org.apache.sshd.server.session.ServerSession session) {
				return true;
			}
		});
		server.setPort(0);
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
		server.setSubsystemFactories(Collections.<NamedFactory<Command>>singletonList(new SftpSubsystem.Factory()));
		server.setFileSystemFactory(new VirtualFileSystemFactory(remoteTemporaryFolder.getRoot().getAbsolutePath()));
		server.start();
		port = server.getPort();
	}

	public static SessionFactory<LsEntry> sessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		factory.setHost("localhost");
		factory.setPort(port);
		factory.setUser("foo");
		factory.setPassword("foo");
		factory.setAllowUnknownKeys(true);
		return new CachingSessionFactory<LsEntry>(factory);
	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
		File hostkey = new File("hostkey.ser");
		if (hostkey.exists()) {
			hostkey.delete();
		}
	}

}
