/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.sftp.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * *
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 * @author Darryl Smith
 *
 * @since 4.1
 *
 */
public class SftpServerTests {

	@Test
	public void testUcPw() throws Exception {
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPasswordAuthenticator((arg0, arg1, arg2) -> true);
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			final String pathname = System.getProperty("java.io.tmpdir") + File.separator + "sftptest" + File.separator;
			new File(pathname).mkdirs();
			server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(pathname)));
			server.start();

			DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
			f.setHost("localhost");
			f.setPort(server.getPort());
			f.setUser("user");
			f.setPassword("pass");
			f.setAllowUnknownKeys(true);
			Session<SftpClient.DirEntry> session = f.getSession();
			doTest(server, session);

			f.destroy();
		}
	}

	@Test
	public void testPubPrivKey() throws Exception {
		testKeyExchange("id_rsa.pub", "id_rsa", null);
	}

	@Test
	public void testPubPrivKeyPassphrase() throws Exception {
		testKeyExchange("id_rsa_pp.pub", "id_rsa_pp", "secret");
	}

	private void testKeyExchange(String pubKey, String privKey, String passphrase) throws Exception {
		final PublicKey allowedKey = decodePublicKey(pubKey);
		try (SshServer server = SshServer.setUpDefaultServer()) {
			server.setPublickeyAuthenticator((username, key, session) -> key.equals(allowedKey));
			server.setPort(0);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
			server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
			final String pathname = System.getProperty("java.io.tmpdir") + File.separator + "sftptest" + File.separator;
			new File(pathname).mkdirs();
			server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(pathname)));
			server.start();

			DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
			f.setHost("localhost");
			f.setPort(server.getPort());
			f.setUser("user");
			f.setAllowUnknownKeys(true);
			InputStream stream = new ClassPathResource(privKey).getInputStream();
			f.setPrivateKey(new ByteArrayResource(FileCopyUtils.copyToByteArray(stream)));
			f.setPrivateKeyPassphrase(passphrase);
			Session<SftpClient.DirEntry> session = f.getSession();
			doTest(server, session);

			f.destroy();
		}
	}

	private PublicKey decodePublicKey(String key) throws Exception {
		InputStream stream = new ClassPathResource(key).getInputStream();
		byte[] keyBytes = FileCopyUtils.copyToByteArray(stream);
		// strip any newline chars
		while (keyBytes[keyBytes.length - 1] == 0x0a || keyBytes[keyBytes.length - 1] == 0x0d) {
			keyBytes = Arrays.copyOf(keyBytes, keyBytes.length - 1);
		}
		byte[] decodeBuffer = Base64.getDecoder().decode(keyBytes);
		ByteBuffer bb = ByteBuffer.wrap(decodeBuffer);
		int len = bb.getInt();
		byte[] type = new byte[len];
		bb.get(type);
		if ("ssh-rsa".equals(new String(type))) {
			BigInteger e = decodeBigInt(bb);
			BigInteger m = decodeBigInt(bb);
			RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
			return KeyFactory.getInstance("RSA").generatePublic(spec);

		}
		else {
			throw new IllegalArgumentException("Only supports RSA");
		}
	}

	private BigInteger decodeBigInt(ByteBuffer bb) {
		int len = bb.getInt();
		byte[] bytes = new byte[len];
		bb.get(bytes);
		return new BigInteger(bytes);
	}

	protected void doTest(SshServer server, Session<SftpClient.DirEntry> session) throws IOException {
		assertThat(server.getActiveSessions().size()).isEqualTo(1);
		SftpClient.DirEntry[] list = session.list(".");
		for (SftpClient.DirEntry entry : list) {
			if (entry.getAttributes().isRegularFile()) {
				session.remove(entry.getFilename());
			}
		}

		session.write(new ByteArrayInputStream("foo".getBytes()), "bar");
		list = session.list(".");
		assertThat(list[1].getFilename()).isEqualTo("bar");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		session.read("bar", outputStream);
		assertThat(new String(outputStream.toByteArray())).isEqualTo("foo");
		session.remove("bar");
		session.close();
	}

}
