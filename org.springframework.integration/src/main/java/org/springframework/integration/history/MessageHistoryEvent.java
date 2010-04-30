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

import java.io.Serializable;

/**
 * Metadata about a historically relevant messaging event along
 * with a timestamp that is generated when this event is created.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistoryEvent implements Serializable {

	private final String name;

	private final String type;

	private final long timestamp;


	/**
	 * Create a MessageHistoryEvent with the metadata of the source component.
	 */
	public MessageHistoryEvent(String name, String type) {
		this.name = name;
		this.type = type;
		this.timestamp = System.currentTimeMillis();
	}


	public String getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Returns the timestamp generated when this event was created.
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.type != null) {
			sb.append(type + ":");
		}
		if (this.name != null) {
			sb.append(name);
			//sb.append("[" + timestamp + "]");
		}
		return sb.toString();
	}

}
