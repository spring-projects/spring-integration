/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.core;

import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Base interface for any source of {@link Message Messages} that can be polled.
 *
 * @param <T> the expected payload type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
@FunctionalInterface
public interface MessageSource<T> extends IntegrationPattern {

	/**
	 * Retrieve the next available message from this source.
	 * Returns {@code null} if no message is available.
	 * @return The message or null.
	 */
	@Nullable
	Message<T> receive();

	@Override
	default IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.inbound_channel_adapter;
	}

}
