/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.function.BiFunction;

import org.springframework.messaging.Message;

/**
 * A contract which can be implemented on the {@link ReleaseStrategy}
 * and used in the {@link AbstractCorrelatingMessageHandler} to
 * populate the provided group condition supplier.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 *
 * @see AbstractCorrelatingMessageHandler#setGroupConditionSupplier(BiFunction)
 */
@FunctionalInterface
public interface GroupConditionProvider {

	BiFunction<Message<?>, String, String> getGroupConditionSupplier();

}
