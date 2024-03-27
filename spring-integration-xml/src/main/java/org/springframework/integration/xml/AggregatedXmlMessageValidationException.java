/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.xml;

import java.util.Collections;
import java.util.List;

/**
 * The validation exception which aggregate all the XML validation errors.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AggregatedXmlMessageValidationException extends RuntimeException {

	private final List<Throwable> exceptions;

	public AggregatedXmlMessageValidationException(List<Throwable> exceptions) {
		this.exceptions = (exceptions != null) ? exceptions : Collections.emptyList();
	}

	@Override
	public String getMessage() {
		StringBuilder message = new StringBuilder("Multiple causes:\n");
		for (Throwable exception : this.exceptions) {
			message.append("    ")
					.append(exception.getMessage())
					.append("\n");
		}
		return message.toString();
	}

	public List<Throwable> getExceptions() {
		return Collections.unmodifiableList(this.exceptions);
	}

}
