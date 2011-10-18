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
package org.springframework.integration.sftp.session;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.StopWatch;

/**
 * @author Oleg Zhurakousky
 *
 */
public class SftpTestSessionFactory {

	public static Session createSftpSession(com.jcraft.jsch.Session jschSession){
		SftpSession sftpSession = new SftpSession(jschSession);
		sftpSession.connect();
		return sftpSession;
	}
	
	@Test
	@Ignore
	public void testPerformance() throws Exception{
		DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
		sessionFactory.setHost("localhost");
		sessionFactory.setPrivateKey(new ClassPathResource("org/springframework/integration/sftp/config/sftp_rsa"));
		sessionFactory.setPrivateKeyPassphrase("springintegration");
		sessionFactory.setPort(22);
		sessionFactory.setUser("user");

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
