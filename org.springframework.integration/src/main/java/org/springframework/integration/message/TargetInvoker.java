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
 * A helper class for sending {@link Message}s to a {@link MessageTarget}.
 *  
 * @author Mark Fisher
 */
public class TargetInvoker {

	private final Log logger = LogFactory.getLog(this.getClass());


	public boolean invoke(MessageTarget target, Message<?> message, long timeout) {
		boolean sent = (target instanceof BlockingTarget && timeout >= 0)
				? ((BlockingTarget) target).send(message, timeout)
				: target.send(message);
		if (!sent && this.logger.isTraceEnabled()) {
			this.logger.trace("failed to send message to target '" + target + "' within timeout: " + timeout);
		}
		return sent;
	}

}
