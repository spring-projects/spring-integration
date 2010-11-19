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

package org.springframework.integration.file.remote.session;

import java.io.InputStream;
import java.util.Collection;

/**
 * Common abstraction for a Session with a remote File system.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Mark Fisher
 * @since 2.0
 */
public interface Session {

	void connect();

	void disconnect();

	boolean exists(String path);

	boolean rm(String path);

	<F> Collection<F> ls(String path);

	InputStream get(String source);

	void put(InputStream inputStream, String destination);

}
