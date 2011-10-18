/*
 * Copyright 2002-2010 the original author or authors.
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

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.StopWatch;

import static junit.framework.Assert.fail;

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
	public void testPerformance() throws Exception{
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		sessionFactory.setHost("192.168.28.143");
		sessionFactory.setUsername("user");
		sessionFactory.setPassword("password");

		Session session = sessionFactory.getSession();
		
		String remoteDir = ".";
		StopWatch existWatch = new StopWatch();
		existWatch.start();
		session.isDirExists(remoteDir);
		existWatch.stop();
		
		StopWatch listWatch = new StopWatch();
		listWatch.start();
		session.list(remoteDir);
		listWatch.stop();
		System.out.println("Elapsed time for directoy exists call: via isDirExists(): " + existWatch.getTotalTimeMillis() + " mls; via list(): " + listWatch.getLastTaskTimeMillis() + " mls;");
	}
}
