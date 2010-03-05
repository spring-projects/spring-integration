/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.history;

import org.springframework.integration.support.ComponentMetadata;

/**
 * Metadata about a historically relevant messaging event along
 * with a timestamp that is generated when this event is created.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistoryEvent extends ComponentMetadata {

	public static final String TIMESTAMP = "timestamp";


	/**
	 * Create a MessageHistoryEvent with the metadata of the source component.
	 */
	public MessageHistoryEvent(ComponentMetadata metadata) {
		super(metadata);
		this.setAttribute(TIMESTAMP, System.currentTimeMillis());
	}


	/**
	 * Returns the timestamp generated when this event was created.
	 */
	public long getTimestamp() {
		return this.getAttribute(TIMESTAMP, long.class);
	}

}
