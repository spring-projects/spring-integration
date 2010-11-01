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

package org.springframework.integration.xml;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AggregatedXmlMessageValidationException extends RuntimeException {

	private final List<Throwable> exceptions;


	public AggregatedXmlMessageValidationException(List<Throwable> exceptions) {
		this.exceptions = (exceptions != null) ? exceptions : Collections.<Throwable>emptyList();
	}


	/**
	 * Returns an Iterator for the aggregated Exceptions.
	 */
	public Iterator<Throwable> exceptionIterator() {
		return exceptions.iterator();
	}

}
