/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.sftp;

import java.io.File;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.server.ApacheMinaSftpEventListener;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

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

	private static final ApacheMinaSftpEventListener EVENT_LISTENER
			= new ApacheMinaSftpEventListener();

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
		EVENT_LISTENER.setApplicationEventPublisher((ev) -> {
			// no-op
		});
		sftpFactory.addSftpEventListener(EVENT_LISTENER);
		server.setSubsystemFactories(Collections.singletonList(sftpFactory));
		server.setFileSystemFactory(new VirtualFileSystemFactory(getRemoteTempFolder().toPath()));
		server.start();
		port = server.getPort();
	}

	public static SessionFactory<SftpClient.DirEntry> sessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(false);
		factory.setHost("localhost");
		factory.setPort(port);
		factory.setUser("foo");
		factory.setPassword("foo");
		factory.setAllowUnknownKeys(true);
		CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory = new CachingSessionFactory<>(factory);
		cachingSessionFactory.setTestSession(true);
		return cachingSessionFactory;
	}

	public static ApacheMinaSftpEventListener eventListener() {
		return EVENT_LISTENER;
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
