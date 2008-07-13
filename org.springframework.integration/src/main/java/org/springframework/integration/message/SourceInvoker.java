/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A helper class for receiving {@link Message}s from a {@link MessageSource}.
 * 
 * @author Mark Fisher
 */
public class SourceInvoker {

	private final Log logger = LogFactory.getLog(this.getClass());


	public Message<?> invoke(MessageSource<?> source, long timeout) {
		Message<?> message = (source instanceof BlockingSource && timeout >= 0)
				? ((BlockingSource<?>) source).receive(timeout)
				: source.receive();
		if (message == null && this.logger.isTraceEnabled()) {
			this.logger.trace("failed to receive message from source '" + source + "' within timeout: " + timeout);
		}
		return message;
	}

}
