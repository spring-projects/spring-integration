/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.sftp.session;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.common.util.Base64;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.util.StreamUtils;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * @author Gary Russell
 * @author David Liu
 * @since 4.1
 *
 */
public class SftpServerTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testUcPw() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		SshServer server = SshServer.setUpDefaultServer();
		try {
			server.setPasswordAuthenticator(new PasswordAuthenticator() {

				@Override
				public boolean authenticate(String arg0, String arg1, ServerSession arg2) {
					return true;
				}
			});
			server.setPort(port);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
			SftpSubsystem.Factory sftp = new SftpSubsystem.Factory();
			server.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(sftp));
			server.setFileSystemFactory(new NativeFileSystemFactory() {

				@Override
				public FileSystemView createFileSystemView(org.apache.sshd.common.Session session) {
					final String pathname = System.getProperty("java.io.tmpdir") + File.separator + "sftptest"
							+ File.separator;
					File f = new File(pathname);
					f.mkdirs();
					return new NativeFileSystemView(session.getUsername(), false) {

						@Override
						public String getVirtualUserDir() {
							return pathname;
						}
					};
				}

			});
			server.start();

			DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
			f.setHost("localhost");
			f.setPort(port);
			f.setUser("user");
			f.setPassword("pass");
			Session<LsEntry> session = f.getSession();
			doTest(server, session);
		}
		finally {
			server.stop(true);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPubPrivKey() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		SshServer server = SshServer.setUpDefaultServer();
		final PublicKey allowedKey = decodePublicKey();
		try {
			server.setPublickeyAuthenticator(new PublickeyAuthenticator() {

				@Override
				public boolean authenticate(String username, PublicKey key, ServerSession session) {
					return key.equals(allowedKey);
				}

			});
			server.setPort(port);
			server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
			SftpSubsystem.Factory sftp = new SftpSubsystem.Factory();
			server.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(sftp));
			server.setFileSystemFactory(new NativeFileSystemFactory() {

				@Override
				public FileSystemView createFileSystemView(org.apache.sshd.common.Session session) {
					final String pathname = System.getProperty("java.io.tmpdir") + File.separator + "sftptest"
							+ File.separator;
					File f = new File(pathname);
					f.mkdirs();
					return new NativeFileSystemView(session.getUsername(), false) {

						@Override
						public String getVirtualUserDir() {
							return pathname;
						}
					};
				}

			});
			server.start();

			DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
			f.setHost("localhost");
			f.setPort(port);
			f.setUser("user");
			InputStream stream = new ClassPathResource("id_rsa").getInputStream();
			f.setPrivateKey(new ByteArrayResource(StreamUtils.copyToByteArray(stream)));
			Session<LsEntry> session = f.getSession();
			doTest(server, session);
		}
		finally {
			server.stop(true);
		}
	}

	private PublicKey decodePublicKey() throws Exception {
		InputStream stream = new ClassPathResource("id_rsa.pub").getInputStream();
		byte[] decodeBuffer = Base64.decodeBase64(StreamUtils.copyToByteArray(stream));
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

	protected void doTest(SshServer server, Session<LsEntry> session) throws IOException {
		assertEquals(1, server.getActiveSessions().size());
		LsEntry[] list = session.list(".");
		if (list.length > 0) {
			session.remove("*");
		}
		session.write(new ByteArrayInputStream("foo".getBytes()), "bar");
		list = session.list(".");
		assertEquals("bar", list[0].getFilename());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		session.read("bar", outputStream);
		assertEquals("foo", new String(outputStream.toByteArray()));
		session.remove("bar");
		session.close();
	}

}
