/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistory implements Iterable<MessageHistory.Event>, Serializable {

	private final List<Event> events = new CopyOnWriteArrayList<Event>();


	public void add(ComponentType componentType, String componentName) {
		this.events.add(new Event(componentType, componentName));
	}

	public Iterator<Event> iterator() {
		return Collections.unmodifiableList(this.events).iterator();
	}

	public String toString() {
		return this.events.toString();
	}


	public static enum ComponentType {
		channel, endpoint;
	}


	public static class Event implements Serializable {

		private final ComponentType componentType;

		private final String componentName;

		private final long timestamp;


		public Event(ComponentType componentType, String componentName) {
			this.componentType = componentType;
			this.componentName = componentName;
			this.timestamp = System.currentTimeMillis();
		}


		public ComponentType getComponentType() {
			return this.componentType;
		}

		public String getComponentName() {
			return this.componentName;
		}

		public long getTimestamp() {
			return this.timestamp;
		}

		public String toString() {
			return "[name=" + this.componentName + ";type=" + this.componentType + ";timestamp=" + this.timestamp + "]";
		}
	}

}
