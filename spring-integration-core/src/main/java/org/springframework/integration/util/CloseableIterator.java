/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.integration.util;

import java.util.Iterator;

/**
 * A {@link CloseableIterator} is intended to be used when it may hold resources (such as file or socket handles).
 * This allows implementations to clean up any resources they need to keep open to iterate over elements.
 *
 * @param <E> the iterator element type.
 *
 * @author Ruslan Stelmachenko
 * @author Gary Russell
 *
 * @since 4.3.15
 */
public interface CloseableIterator<E> extends Iterator<E>, AutoCloseable {

	@Override
	void close(); // override throws Exception

}
