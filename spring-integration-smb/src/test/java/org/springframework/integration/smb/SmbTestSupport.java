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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.smb.session.SmbSessionFactory;

import jcifs.DialectVersion;
import jcifs.smb.SmbFile;

/**
 * Provides a connection to an external SMB Server for test cases.
 *
 * The constants need to be updated with the 'real' server settings for testing.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */

@Disabled("Actual SMB share must be configured in class [SmbTestSupport].")
public class SmbTestSupport extends RemoteFileTestSupport {

	public static final String HOST = "localhost";

	public static final String SHARE_AND_DIR = "smb-share/";

	public static final String USERNAME = "sambaguest";

	public static final String PASSWORD = "sambaguest";

	private static SmbSessionFactory smbSessionFactory;

	@BeforeAll
	public static void connectToSMBServer() throws Exception {
		smbSessionFactory = new SmbSessionFactory();
		smbSessionFactory.setHost(HOST);
		smbSessionFactory.setUsername(USERNAME);
		smbSessionFactory.setPassword(PASSWORD);
		smbSessionFactory.setShareAndDir(SHARE_AND_DIR);
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
