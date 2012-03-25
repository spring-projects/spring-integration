/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;

/**
 * Temporary extension to {@link Session} to avoid a breaking change
 * in a point release - merge with Session in 2.2.
 * @author Gary Russell
 * @since 2.1.1
 *
 */
public interface ExtendedSession<T> extends Session<T> {

	String[] listNames(String path) throws IOException;
}
