/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.stream;

/**
 * Application event published when EOF is detected on a stream.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.stream.event.StreamClosedEvent}
 */
@SuppressWarnings("serial")
@Deprecated(forRemoval = true, since = "7.0")
public class StreamClosedEvent extends org.springframework.integration.stream.event.StreamClosedEvent {

	public StreamClosedEvent(Object source) {
		super(source);
	}

}
