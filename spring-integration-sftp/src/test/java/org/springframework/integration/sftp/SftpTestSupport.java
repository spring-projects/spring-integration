/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.sftp;

import java.io.File;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.server.ApacheMinaSftpEventListener;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * Provides an embedded SFTP Server for test cases.
 *
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class SftpTestSupport extends RemoteFileTestSupport {

	private static SshServer server;

	private static ApacheMinaSftpEventListener eventListener = new ApacheMinaSftpEventListener();

	@Override
	public String getTargetLocalDirectoryName() {
		return targetLocalDirectory.getAbsolutePath() + File.separator;
	}

	@Override
	public String prefix() {
		return "sftp";
	}

	@BeforeAll
	public static void createServer() throws Exception {
		server = SshServer.setUpDefaultServer();
		server.setPasswordAuthenticator((username, password, session) -> true);
		server.setPort(0);
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
		SftpSubsystemFactory sftpFactory = new SftpSubsystemFactory();
		eventListener.setApplicationEventPublisher((ev) -> {
			// no-op
		});
		sftpFactory.addSftpEventListener(eventListener);
		server.setSubsystemFactories(Collections.singletonList(sftpFactory));
		server.setFileSystemFactory(new VirtualFileSystemFactory(getRemoteTempFolder().toPath()));
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
		return new CachingSessionFactory<>(factory);
	}

	public static ApacheMinaSftpEventListener eventListener() {
		return eventListener;
	}

	@AfterAll
	public static void stopServer() throws Exception {
		server.stop();
		File hostKey = new File("hostkey.ser");
		if (hostKey.exists()) {
			hostKey.delete();
		}
	}

}
