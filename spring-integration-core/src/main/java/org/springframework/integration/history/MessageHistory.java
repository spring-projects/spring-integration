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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.Message;
import org.springframework.util.StringUtils;

/**
 * Threadsafe Iterable list of {@link MessageHistoryEvent} instances.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Iwein Fuld
 * @since 2.0
 */
@SuppressWarnings("serial")
public class MessageHistory implements Iterable<MessageHistoryEvent>, Serializable {

	private final Queue<MessageHistoryEvent> events = new ConcurrentLinkedQueue<MessageHistoryEvent>();

	/**
	 * Add a new event with the provided component metadata.
	 */
    public MessageHistoryEvent addEvent(NamedComponent component) {
    	String name = component.getComponentName();
    	String type = component.getComponentType();
        if (name != null && !StringUtils.startsWithIgnoreCase(name, "org.springframework")) {
            MessageHistoryEvent event = new MessageHistoryEvent(name, type);
            this.events.add(event);
            return event;
        }
        return null;
    }

    /**
     * Returns a weakly consistent iterator that will never throw
     * ConcurrentModificationException as in {@link java.util.concurrent.ConcurrentLinkedQueue#iterator()}.
     */
    public Iterator<MessageHistoryEvent> iterator() {
        return this.events.iterator();
    }

	public boolean equals(Object other) {
		return (other instanceof MessageHistory
				&& this.events.containsAll(((MessageHistory) other).events))
                && ((MessageHistory) other).events.containsAll(this.events);
	}

	public int hashCode() {
        return 17 * this.events.hashCode();
	}

	/**
	 * Returns a String representation of the history event list.
	 */
	public String toString() {
        return new ArrayList<MessageHistoryEvent>(events).toString();
    }
}
