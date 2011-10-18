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
import org.junit.Test;

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
}
