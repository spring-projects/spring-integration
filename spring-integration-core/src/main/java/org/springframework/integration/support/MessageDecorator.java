/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.support;

import org.springframework.messaging.Message;

/**
 * The {@link Message} decoration contract.
 * An implementation may decide to return any {@link Message} instance
 * and even a different {@link Message} implementation. Usually used to
 * wrap a message in another.
 *
 * @author Artem Bilan
 * @since 4.2.9
 */
@FunctionalInterface
public interface MessageDecorator {

	Message<?> decorateMessage(Message<?> message);

}
