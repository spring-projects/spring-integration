/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.message;

import org.springframework.integration.util.RandomUuidGenerator;
import org.springframework.integration.util.IdGenerator;
import org.springframework.util.Assert;

/**
 * Base class that provides the default {@link IdGenerator} as well as a setter
 * for providing a custom id generator implementation.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageMapper<M, O> implements MessageMapper<M, O> {

	private IdGenerator idGenerator = new RandomUuidGenerator();


	public void setIdGenerator(IdGenerator idGenerator) {
		Assert.notNull(idGenerator, "'idGenerator' must not be null");
		this.idGenerator = idGenerator;
	}

	protected IdGenerator getIdGenerator() {
		return this.idGenerator;
	}

}
