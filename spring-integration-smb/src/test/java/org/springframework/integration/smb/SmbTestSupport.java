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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.integration.file.remote.RemoteFileTestSupport;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.smb.session.SmbSessionFactory;

import jcifs.DialectVersion;
import jcifs.smb.SmbFile;

/**
 * Provides a connection to an external SMB Server for test cases.
 *
 * The actual SMB share must be configured in file 'SmbTests-context.xml'
 * with the 'real' server settings for testing.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */

@Disabled("Actual SMB share must be configured in file [SmbTests-context.xml].")
public class SmbTestSupport extends RemoteFileTestSupport {

	private static GenericXmlApplicationContext context = new GenericXmlApplicationContext();

	private static SmbSessionFactory smbSessionFactory;

	@BeforeAll
	public static void connectToSMBServer() throws Exception {
		context.load("classpath:META-INF/spring/integration/SmbTests-context.xml");
		context.registerShutdownHook();
		context.refresh();

		smbSessionFactory = context.getBean("smbSessionFactory", SmbSessionFactory.class);
		smbSessionFactory.setSmbMinVersion(DialectVersion.SMB210);
		smbSessionFactory.setSmbMaxVersion(DialectVersion.SMB311);
	}

	@AfterAll
	public static void cleanUp() {
		context.close();
	}

	public static SessionFactory<SmbFile> sessionFactory() {
		return new CachingSessionFactory<>(smbSessionFactory);
	}

	@Override
	protected String prefix() {
		return "smb";
	}

}
