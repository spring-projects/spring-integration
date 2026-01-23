/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ftp.session;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.PoolItemNotAvailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 */
class SessionFactoryTests {

	@Test
	void testFtpClientInteraction() throws Exception {
		final FTPClient client = mock(FTPClient.class);
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory() {

			@Override
			protected FTPClient createClientInstance() {
				return client;
			}
		};
		sessionFactory.setUsername("foo");
		sessionFactory.setConnectTimeout(123);
		sessionFactory.setDefaultTimeout(456);
		sessionFactory.setDataTimeout(789);
		doReturn(200).when(client).getReplyCode();
		doReturn(true).when(client).login("foo", null);
		FtpSession session = sessionFactory.getSession();
		verify(client).setConnectTimeout(123);
		verify(client).setDefaultTimeout(456);
		verify(client).setDataTimeout(Duration.ofMillis(789));

		session.close();

		verify(client).logout();
		verify(client).disconnect();

		doThrow(RuntimeException.class).when(client).noop();

		session.close();

		verify(client).logout();
		verify(client, times(2)).disconnect();
	}

	@Test
	void testWithControlEncoding() {
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		sessionFactory.setControlEncoding("UTF-8");
		assertThat(TestUtils.<String>getPropertyValue(sessionFactory, "controlEncoding"))
				.as("Expected controlEncoding value of 'UTF-8'").isEqualTo("UTF-8");
	}

	@Test
	void testWithoutControlEncoding() {
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		assertThat(TestUtils.<String>getPropertyValue(sessionFactory, "controlEncoding"))
				.as("Expected controlEncoding value of 'ISO-8859-1'").isEqualTo("ISO-8859-1");
	}

	@Test
	void testEmptyControlEncoding() {
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> sessionFactory.setControlEncoding(""));
	}

	@Test
	void testNullControlEncoding() {
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> sessionFactory.setControlEncoding(null));
	}

	@Test
	void testClientModes() throws Exception {
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		Field[] fields = FTPClient.class.getDeclaredFields();
		for (Field field : fields) {
			if (field.getName().endsWith("MODE")) {
				try {
					int clientMode = field.getInt(null);
					sessionFactory.setClientMode(clientMode);
					if (!(clientMode == FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE ||
							clientMode == FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE)) {
						fail("IllegalArgumentException expected");
					}
				}
				catch (IllegalArgumentException e) {
					// success
				}
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void testStaleConnection() {
		SessionFactory<FTPFile> sessionFactory = Mockito.mock(SessionFactory.class);
		Session<FTPFile> sessionA = Mockito.mock(Session.class);
		Session<FTPFile> sessionB = Mockito.mock(Session.class);
		Mockito.when(sessionA.isOpen()).thenReturn(true);
		Mockito.when(sessionB.isOpen()).thenReturn(false);

		Mockito.when(sessionFactory.getSession()).thenReturn(sessionA);
		Mockito.when(sessionFactory.getSession()).thenReturn(sessionB);

		CachingSessionFactory<FTPFile> cachingFactory = new CachingSessionFactory<>(sessionFactory, 2);

		Session<FTPFile> firstSession = cachingFactory.getSession();
		Session<FTPFile> secondSession = cachingFactory.getSession();
		secondSession.close();
		Session<FTPFile> nonStaleSession = cachingFactory.getSession();
		assertThat(TestUtils.<Object>getPropertyValue(nonStaleSession, "targetSession"))
				.isEqualTo(TestUtils.getPropertyValue(firstSession, "targetSession"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testSameSessionFromThePool() {
		SessionFactory<FTPFile> sessionFactory = Mockito.mock(SessionFactory.class);
		Session<FTPFile> session = Mockito.mock(Session.class);
		Mockito.when(sessionFactory.getSession()).thenReturn(session);

		CachingSessionFactory<FTPFile> cachingFactory = new CachingSessionFactory<>(sessionFactory, 2);

		Session<FTPFile> s1 = cachingFactory.getSession();
		s1.close();
		Session<FTPFile> s2 = cachingFactory.getSession();
		s2.close();
		assertThat(TestUtils.<Object>getPropertyValue(s2, "targetSession"))
				.isEqualTo(TestUtils.getPropertyValue(s1, "targetSession"));
		Mockito.verify(sessionFactory, Mockito.times(2)).getSession();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testSessionWaitExpire() {
		SessionFactory<FTPFile> sessionFactory = Mockito.mock(SessionFactory.class);
		Session<FTPFile> session = Mockito.mock(Session.class);
		Mockito.when(sessionFactory.getSession()).thenReturn(session);

		CachingSessionFactory<FTPFile> cachingFactory = new CachingSessionFactory<>(sessionFactory, 2);

		cachingFactory.setSessionWaitTimeout(3000);

		cachingFactory.getSession();
		cachingFactory.getSession();

		assertThatExceptionOfType(PoolItemNotAvailableException.class) // timeout expire
				.isThrownBy(cachingFactory::getSession);
	}

	@Test
	@Disabled
	void testConnectionLimit() throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		sessionFactory.setHost("192.168.28.143");
		sessionFactory.setPassword("password");
		sessionFactory.setUsername("user");
		final CachingSessionFactory<FTPFile> factory = new CachingSessionFactory<>(sessionFactory, 2);

		final Random random = new Random();
		final AtomicInteger failures = new AtomicInteger();
		for (int i = 0; i < 30; i++) {
			executor.execute(() -> {
				try {
					Session<FTPFile> session = factory.getSession();
					Thread.sleep(random.nextInt(5000));
					session.close();
				}
				catch (Exception e) {
					failures.incrementAndGet();
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(10000, TimeUnit.SECONDS);

		assertThat(failures.get()).isEqualTo(0);
	}

}
