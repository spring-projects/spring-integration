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

import org.springframework.context.Lifecycle;

/**
 * Holds instances of {@link SftpSession} since they are stateful
 * and might be in use while another operation runs.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public interface SftpSessionPool extends Lifecycle{

	/**
	 * Returns a session that can be used to connect to an sftp instance and perform operations
	 *
	 * @return the session from the pool ready to be connected to.
	 * @throws Exception if any fault occurs when trying to connect to the remote server
	 */
	SftpSession getSession() throws Exception;

	/**
	 * Releases the session.
	 * 
	 * @param session the session to relinquish / renew
	 */
	void release(SftpSession session);
	
}
