/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.jdbc.lock;

import java.io.Closeable;

/**
 * Encapsulation of the SQL shunting that is needed for locks. A {@link JdbcLockRegistry}
 * needs a reference to a spring-managed (transactional) client service, so this component
 * has to be declared as a bean.
 *
 * @author Dave Syer
 *
 */
public interface JdbcClient extends Closeable {

	boolean isAcquired(String lock);

	void delete(String lock);

	boolean acquire(String lock);

	@Override
	void close();

}
