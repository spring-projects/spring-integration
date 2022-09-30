/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.integration.core.GenericTransformer;
import org.springframework.messaging.Message;

/**
 * Strategy interface for transforming a {@link Message}.
 *
 * @author Mark Fisher
 */
@FunctionalInterface
public interface Transformer extends GenericTransformer<Message<?>, Message<?>> {

	Message<?> transform(Message<?> message);

}
