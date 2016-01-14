/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.router;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.ClassUtils;

/**
 * A Message Router that resolves the target {@link MessageChannel} for
 * messages whose payload is an Exception. The channel resolution is based upon
 * the most specific cause of the error for which a channel-mapping exists.
 * <p>
 * The channel-mapping can be specified on the super classes to avoid mapping duplication
 * for the particular exception implementation.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ErrorMessageExceptionTypeRouter extends AbstractMappingMessageRouter {

	@Override
	protected List<Object> getChannelKeys(Message<?> message) {
		String mostSpecificCause = null;
		Object payload = message.getPayload();
		if (payload instanceof Throwable) {
			Throwable cause = (Throwable) payload;
			Set<String> mapping = getChannelMappings().keySet();
			Map<String, Class<?>> classesCache = new HashMap<String, Class<?>>();
			while (cause != null) {
				for (String className : mapping) {
					Class<?> exceptionClass = classesCache.get(className);
					if (exceptionClass == null) {
						try {
							exceptionClass = ClassUtils.forName(className, getApplicationContext().getClassLoader());
							classesCache.put(className, exceptionClass);
						}
						catch (ClassNotFoundException e) {
							throw new IllegalStateException(e);
						}
					}
					if (exceptionClass.isInstance(cause)) {
						mostSpecificCause = className;
					}
				}
				cause = cause.getCause();
			}
		}
		return mostSpecificCause != null ? Collections.<Object>singletonList(mostSpecificCause) : null;
	}

}
