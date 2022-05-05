/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.integration.smb.session;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import jcifs.DialectVersion;
import jcifs.smb.SmbFile;

/**
 *
 * @author Gunnar Hillert
 * @author Gregory Bragg
 *
 */
public class SmbSessionTests {

	@Test
	public void testCreateSmbFileObjectWithBackSlash1() throws IOException {
		System.setProperty("file.separator", "\\");
		SmbConfig config = new SmbConfig();
		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared/");
		SmbShare smbShare = new SmbShare(config);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("smb://myshare\\blubba\\");
		assertEquals("smb://myshare/blubba/", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithBackSlash2() throws IOException {
		System.setProperty("file.separator", "\\");
		SmbConfig config = new SmbConfig();
		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared\\");
		SmbShare smbShare = new SmbShare(config);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("smb://myshare\\blubba\\");
		assertEquals("smb://myshare/blubba/", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithBackSlash3() throws IOException {
		System.setProperty("file.separator", "\\");
		SmbConfig config = new SmbConfig();
		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared\\");
		SmbShare smbShare = new SmbShare(config);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("..\\another");
		assertEquals("smb://myshare:445/another", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithBackSlash4() throws IOException {
		System.setProperty("file.separator", "/");
		SmbConfig config = new SmbConfig();
		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared/");
		SmbShare smbShare = new SmbShare(config);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("smb://myshare\\blubba\\");
		assertEquals("smb://myshare/blubba/", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithMissingTrailingSlash1() throws IOException {
		SmbConfig config = new SmbConfig();
		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared");
		SmbShare smbShare = new SmbShare(config);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("smb://myshare\\blubba");
		assertEquals("smb://myshare/blubba", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithMissingTrailingSlash2() throws IOException {
		SmbConfig config = new SmbConfig();
		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared/");
		SmbShare smbShare = new SmbShare(config);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject(".");
		assertEquals("smb://myshare:445/shared/", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithMissingTrailingSlash3() throws IOException {
		SmbConfig config = new SmbConfig();
		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared/");
		SmbShare smbShare = new SmbShare(config);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("../anotherShare");
		assertEquals("smb://myshare:445/anotherShare", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithSmb3Versions1() throws IOException {
		Properties props = new Properties();
		SmbConfig config = new SmbConfig();

		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared/");
		config.setSmbMinVersion(DialectVersion.SMB300);
		config.setSmbMaxVersion(DialectVersion.SMB311);

		props.setProperty("jcifs.smb.client.minVersion", config.getSmbMinVersion().name());
		props.setProperty("jcifs.smb.client.maxVersion", config.getSmbMaxVersion().name());

		SmbShare smbShare = new SmbShare(config, props);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("smb://myshare\\blubba");
		assertEquals("smb://myshare/blubba", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithSmb3Versions2() throws IOException {
		Properties props = new Properties();
		SmbConfig config = new SmbConfig();

		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared/");
		config.setSmbMinVersion(DialectVersion.SMB302);
		config.setSmbMaxVersion(DialectVersion.SMB311);

		props.setProperty("jcifs.smb.client.minVersion", config.getSmbMinVersion().name());
		props.setProperty("jcifs.smb.client.maxVersion", config.getSmbMaxVersion().name());

		SmbShare smbShare = new SmbShare(config, props);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("smb://myshare\\blubba");
		assertEquals("smb://myshare/blubba", smbFile.getPath());
		smbSession.close();
	}

	@Test
	public void testCreateSmbFileObjectWithSmb3Versions3() throws IOException {
		Properties props = new Properties();
		SmbConfig config = new SmbConfig();

		config.setHost("myshare");
		config.setPort(445);
		config.setShareAndDir("shared/");
		config.setSmbMinVersion(DialectVersion.SMB311);
		config.setSmbMaxVersion(DialectVersion.SMB311);

		props.setProperty("jcifs.smb.client.minVersion", config.getSmbMinVersion().name());
		props.setProperty("jcifs.smb.client.maxVersion", config.getSmbMaxVersion().name());

		SmbShare smbShare = new SmbShare(config, props);
		SmbSession smbSession = new SmbSession(smbShare);

		SmbFile smbFile = smbSession.createSmbFileObject("smb://myshare\\blubba");
		assertEquals("smb://myshare/blubba", smbFile.getPath());
		smbSession.close();
	}
}
