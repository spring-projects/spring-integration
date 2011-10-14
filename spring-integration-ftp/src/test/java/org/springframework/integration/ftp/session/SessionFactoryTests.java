/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.ftp.session;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;

import static junit.framework.Assert.fail;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleg Zhurakousky
 *
 */
public class SessionFactoryTests {

	@Test
	public void testClientModes() throws Exception{
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		Field[] fields = FTPClient.class.getDeclaredFields();
		for (Field field : fields) {
			if (field.getName().endsWith("MODE")){
				try {
					int clientMode = field.getInt(null);
					sessionFactory.setClientMode(clientMode);
					if (!(clientMode == FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE || 
						clientMode == FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE)){
						fail();
					}	
				} catch (IllegalArgumentException e) {
					// success
				} catch (Throwable e) {
					fail();
				}
			}
		}	
	}
	@Test
	@Ignore
	public void testConnectionLimit() throws Exception{
		ExecutorService executor = Executors.newCachedThreadPool();
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		sessionFactory.setHost("192.168.28.143");
		sessionFactory.setPassword("password");
		sessionFactory.setUsername("user");
		final CachingSessionFactory factory = new CachingSessionFactory(sessionFactory);
		factory.setSessionCacheSize(2);
		factory.afterPropertiesSet();
		final Random random = new Random();
		final AtomicInteger failures = new AtomicInteger();
		for (int i = 0; i < 30; i++) {
			executor.execute(new Runnable() {	
				public void run() {		
					try {
						Session session = factory.getSession();
						Thread.sleep(random.nextInt(10000));
						session.close();
					} catch (Exception e) {
						e.printStackTrace();
						failures.incrementAndGet();
					}
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(1000, TimeUnit.SECONDS);
		assertEquals(0, failures.get());
	}
}
