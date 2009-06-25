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

package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.OrderComparator;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;

/**
 * {@link AbstractUnicastDispatcher} that will try its handlers in the
 * same order every dispatch.
 * 
 * <p>The {@link Comparator} that determines the order may be provided via the
 * {@link #setComparator(Comparator)} method. If none is provided, the default
 * will be an instance of {@link OrderComparator}, and any {@link MessageHandler}
 * that implements {@link org.springframework.core.Ordered} will be ordered
 * accordingly. Any other handlers will be placed at the end of the list.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class FailOverDispatcher extends AbstractUnicastDispatcher {

	@SuppressWarnings("unchecked")
	private Comparator<Object> comparator = new OrderComparator();


	/**
	 * Provide a {@link Comparator} that will determine the sorting order of
	 * the handler list and hence the order of the failover chain. If no
	 * comparator is configured, the default will be an instance of
	 * {@link OrderComparator}.
	 */
	public void setComparator(Comparator<Object> comparator) {
		Assert.notNull(comparator, "comparator must not be null");
		this.comparator = comparator;
	}

	@Override
	protected Iterator<MessageHandler> getHandlerIterator(List<MessageHandler> handlers) {
		List<MessageHandler> handlerList = new ArrayList<MessageHandler>(handlers);
		Collections.sort(handlerList, this.comparator);
		return handlerList.iterator();
	}

}
