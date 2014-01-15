/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.xml;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AggregatedXmlMessageValidationException extends RuntimeException {

	private final List<Throwable> exceptions;


	public AggregatedXmlMessageValidationException(List<Throwable> exceptions) {
		this.exceptions = (exceptions != null) ? exceptions : Collections.<Throwable>emptyList();
	}

	@Override
	public String getMessage() {
		StringBuilder message = new StringBuilder("Multiple causes:\n");
		for (Throwable exception : this.exceptions) {
			message.append("    " + exception.getMessage() + "\n");
		}
		return message.toString();
	}

	/**
	 * @return The exception iterator.
	 * @deprecated in favor of #getExceptions
	 */
	@Deprecated
	public Iterator<Throwable> exceptionIterator() {
		return exceptions.iterator();
	}

	public List<Throwable> getExceptions() {
		return Collections.unmodifiableList(exceptions);
	}

}
