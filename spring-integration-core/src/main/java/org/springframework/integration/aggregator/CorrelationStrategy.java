/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.aggregator;

import org.springframework.messaging.Message;

/**
 * Strategy for determining how messages can be correlated. Implementations
 * should return the correlation key value associated with a particular message.
 *
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public interface CorrelationStrategy {

	/**
	 * Find the correlation key for the given message. If no key can be determined the strategy should not return
	 * <code>null</code>, but throw an exception.
	 *
	 * @param message The message.
	 * @return The correlation key.
	 */
	Object getCorrelationKey(Message<?> message);

}
