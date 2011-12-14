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
package org.springframework.integration.ftp.inbound;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizerFactoryBean;

/**
 * Factory bean that creates an instance of {@link FtpInboundFileSynchronizer}
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class FtpInboundFileSynchronizerFactoryBean extends AbstractInboundFileSynchronizerFactoryBean<FTPFile>{

	public Class<?> getObjectType() {
		return FtpInboundFileSynchronizer.class;
	}

	@Override
	protected AbstractInboundFileSynchronizer<FTPFile> getInstance(SessionFactory<FTPFile> sessionFactory) {
		return new FtpInboundFileSynchronizer(sessionFactory);
	}
	

}
