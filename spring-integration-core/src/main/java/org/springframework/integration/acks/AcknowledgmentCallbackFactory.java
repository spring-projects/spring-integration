/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.integration.acks;

/**
 * A factory for creating {@link AcknowledgmentCallback}s.
 *
 * @param <T> a type containing information with which to populate the acknowledgment.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
@FunctionalInterface
public interface AcknowledgmentCallbackFactory<T> {

	/**
	 * Create the callback.
	 * @param info information for the callback to process the acknowledgment.
	 * @return the callback
	 */
	AcknowledgmentCallback createCallback(T info);

}
