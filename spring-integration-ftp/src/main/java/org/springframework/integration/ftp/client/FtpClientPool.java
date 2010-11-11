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

package org.springframework.integration.ftp.client;

import org.apache.commons.net.ftp.FTPClient;


/**
 * A pool of {@link FTPClient} instances. The pool can be used to control the
 * number of open FTP connections and reuse these connections efficiently.
 *
 * @author Iwein Fuld
 */
public interface FtpClientPool extends FtpClientFactory<FTPClient> {

	/**
	 * Releases the client back to the pool. When calling this method the caller
	 * is no longer responsible for the connection. The pool is free to do with
	 * it as it sees fit, which means either recycling or disconnecting it most
	 * probably.
	 * <p/>
	 * The caller should NOT disconnect the client before calling this method.
	 * <p/>
	 * The caller is NOT expected to use the client after calling this method.
	 * Doing so can lead to unexpected behavior.
	 *
	 * @param client the {@link FTPClient} to release. Implementations of this
	 *               method are recommended to deal gracefully with a <code>null</code>
	 *               argument, although the endpoint implementations in
	 *               <code>org.springframework.integration.ftp</code> will never pass in
	 *               <code>null</code>.
	 */
	void releaseClient(FTPClient client);

}
