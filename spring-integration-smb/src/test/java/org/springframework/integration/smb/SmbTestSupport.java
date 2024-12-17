/*
 * Copyright 2022-2024 the original author or authors.
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jcifs.DialectVersion;
import jcifs.smb.SmbFile;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.smb.session.SmbSessionFactory;

/**
 * Provides a connection to a Testcontainers-driven SMB Server for test cases.
 *
 * The following folder structures in the SMB share is expected
 * for a successful completion of these unit tests:
 *
 * <pre class="code">
 *  smbSource/
 *  |-- smbSource1.txt - contains 'source1'
 *  |-- smbSource2.txt - contains 'source2'
 *  |-- SMBSOURCE1.TXT.a
 *  |-- SMBSOURCE2.TXT.a
 *  |-- subSmbSource/
 *      |-- subSmbSource1.txt - contains 'subSource1'
 *      |-- subSmbSource2.txt - contains 'subSource2'
 *  |-- subSmbSource2/ - directory will be created in testSmbPutFlow
 *      |-- subSmbSource2-1.txt - file will be created in testSmbPutFlow and deleted in testSmbRmFlow
 *      |-- subSmbSource2-2.txt - file will be created in testSmbMputFlow
 *      |-- subSmbSource2-3.txt - file will be created in testSmbMputFlow and renamed in testSmbMvFlow to subSmbSource-MV-Flow-Renamed.txt
 *  smbTarget/
 * </pre>
 *
 * @author Gregory Bragg
 * @author Artem Vozhdayenko
 * @author Artem Bilan
 *
 * @since 6.0
 */

@Testcontainers(disabledWithoutDocker = true)
public class SmbTestSupport extends RemoteFileTestSupport {

	public static final String HOST = "127.0.0.1";

	public static final String SHARE_AND_DIR = "smb share";

	public static final String USERNAME = "sambaguest";

	public static final String PASSWORD = "sambaguest";

	private static final String INNER_SHARE_DIR = "/tmp";

	private static final GenericContainer<?> SMB_CONTAINER = new GenericContainer<>("elswork/samba:4.15.5")
			.withTmpFs(Map.of(INNER_SHARE_DIR, "rw"))
			.withCommand("-u", "1000:1000:" + USERNAME + ":" + USERNAME + ":" + PASSWORD,
					"-s", SHARE_AND_DIR + ":" + INNER_SHARE_DIR + ":rw:" + USERNAME)
			.withExposedPorts(445);

	protected static SmbSessionFactory smbSessionFactory;

	@BeforeAll
	public static void connectToSMBServer() throws IOException {
		SMB_CONTAINER.start();

		smbSessionFactory = new SmbSessionFactory();
		smbSessionFactory.setHost(HOST);
		smbSessionFactory.setPort(SMB_CONTAINER.getFirstMappedPort());
		smbSessionFactory.setUsername(USERNAME);
		smbSessionFactory.setPassword(PASSWORD);
		smbSessionFactory.setShareAndDir(SHARE_AND_DIR + "/");
		smbSessionFactory.setSmbMinVersion(DialectVersion.SMB210);
		smbSessionFactory.setSmbMaxVersion(DialectVersion.SMB311);

		try (Session<SmbFile> smbFileSession = smbSessionFactory.getSession()) {
			smbFileSession.mkdir("smbTarget");
			Charset charset = StandardCharsets.UTF_8;
			smbFileSession.write(IOUtils.toInputStream("source1", charset), "smbSource/smbSource1.txt");
			smbFileSession.write(IOUtils.toInputStream("source2", charset), "smbSource/smbSource2.txt");
			smbFileSession.write(IOUtils.toInputStream("", charset), "SMBSOURCE1.TXT.a");
			smbFileSession.write(IOUtils.toInputStream("", charset), "SMBSOURCE2.TXT.a");
			smbFileSession.write(IOUtils.toInputStream("subSource1", charset), "smbSource/subSmbSource/subSmbSource1.txt");
			smbFileSession.write(IOUtils.toInputStream("subSource2", charset), "smbSource/subSmbSource/subSmbSource2.txt");
		}
	}

	public static String smbServerUrl() {
		return smbSessionFactory.rawUrl().replaceFirst('/' + SHARE_AND_DIR + '/', "");
	}

	public static SessionFactory<SmbFile> sessionFactory() {
		return new CachingSessionFactory<>(smbSessionFactory);
	}

	@Override
	protected String prefix() {
		return "smb";
	}

}
