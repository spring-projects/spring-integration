/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.smb;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.smb.session.SmbSessionFactory;

import jcifs.DialectVersion;
import jcifs.smb.SmbFile;

/**
 * Provides a connection to a Testcontainers-driven SMB Server for test cases.
 *
 * @author Gregory Bragg
 * @author Artem Vozhdayenko
 *
 * @since 6.0
 */

@Testcontainers(disabledWithoutDocker = true)
public class SmbTestSupport extends RemoteFileTestSupport {

	public static final String HOST = "127.0.0.1";

	public static final String SHARE_AND_DIR = "smb-share";

	public static final String USERNAME = "sambaguest";

	public static final String PASSWORD = "sambaguest";

	private static final String INNER_SHARE_DIR = "/tmp";

	// Configuration details can be found at https://hub.docker.com/r/dperson/samba
	private static final GenericContainer<?> SMB_CONTAINER = new GenericContainer<>("dperson/samba:latest")
			.withExposedPorts(445)
			.withEnv("USER", USERNAME + ";" + PASSWORD)
			.withTmpFs(Map.of(INNER_SHARE_DIR, "rw"))
			.withEnv("SHARE", SHARE_AND_DIR + ";" + INNER_SHARE_DIR + ";yes;no;no;all;none");

	private static SmbSessionFactory smbSessionFactory;

	@BeforeAll
	public static void connectToSMBServer() {
		SMB_CONTAINER.start();

		smbSessionFactory = new SmbSessionFactory();
		smbSessionFactory.setHost(HOST);
		smbSessionFactory.setPort(SMB_CONTAINER.getFirstMappedPort());
		smbSessionFactory.setUsername(USERNAME);
		smbSessionFactory.setPassword(PASSWORD);
		smbSessionFactory.setShareAndDir(SHARE_AND_DIR + "/");
		smbSessionFactory.setSmbMinVersion(DialectVersion.SMB210);
		smbSessionFactory.setSmbMaxVersion(DialectVersion.SMB311);
	}

	public static SessionFactory<SmbFile> sessionFactory() {
		return new CachingSessionFactory<>(smbSessionFactory);
	}

	@Override
	protected String prefix() {
		return "smb";
	}

}
