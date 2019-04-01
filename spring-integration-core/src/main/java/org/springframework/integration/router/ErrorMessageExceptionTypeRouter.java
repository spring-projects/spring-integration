/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.router;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A Message Router that resolves the target
 * {@link org.springframework.messaging.MessageChannel} for
 * messages whose payload is a {@link Throwable}.
 * The channel resolution is based upon the most specific cause
 * of the error for which a channel-mapping exists.
 * <p>
 * The channel-mapping can be specified for the super classes to avoid mapping duplication
 * for the particular exception implementation.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
public class ErrorMessageExceptionTypeRouter extends AbstractMappingMessageRouter {

	private volatile Map<String, Class<?>> classNameMappings = new LinkedHashMap<>();

	private volatile boolean initialized;

	@Override
	@ManagedAttribute
	public void setChannelMappings(Map<String, String> channelMappings) {
		super.setChannelMappings(channelMappings);
		if (this.initialized) {
			populateClassNameMapping(channelMappings.keySet());
		}
	}

	private void populateClassNameMapping(Set<String> classNames) {
		Map<String, Class<?>> newClassNameMappings = new LinkedHashMap<>();
		for (String className : classNames) {
			newClassNameMappings.put(className, resolveClassFromName(className));
		}
		this.classNameMappings = newClassNameMappings;
	}

	private Class<?> resolveClassFromName(String className) {
		try {
			Assert.state(getApplicationContext() != null, "An ApplicationContext is required");
			return ClassUtils.forName(className, getApplicationContext().getClassLoader());
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Cannot load class for channel mapping.", e);
		}
	}

	@Override
	@ManagedOperation
	public void setChannelMapping(String key, String channelName) {
		super.setChannelMapping(key, channelName);
		if (this.initialized) {
			Map<String, Class<?>> newClassNameMappings = new LinkedHashMap<>(this.classNameMappings);
			newClassNameMappings.put(key, resolveClassFromName(key));
			this.classNameMappings = newClassNameMappings;
		}
	}

	@Override
	@ManagedOperation
	public void removeChannelMapping(String key) {
		super.removeChannelMapping(key);
		Map<String, Class<?>> newClassNameMappings = new LinkedHashMap<>(this.classNameMappings);
		newClassNameMappings.remove(key);
		this.classNameMappings = newClassNameMappings;
	}

	@Override
	@ManagedOperation
	public void replaceChannelMappings(Properties channelMappings) {
		super.replaceChannelMappings(channelMappings);
		populateClassNameMapping(getChannelMappings().keySet());
	}

	@Override
	protected void onInit() {
		super.onInit();
		populateClassNameMapping(getChannelMappings().keySet());
		this.initialized = true;
	}

	@Override
	protected List<Object> getChannelKeys(Message<?> message) {
		String mostSpecificCause = null;
		Object payload = message.getPayload();
		if (payload instanceof Throwable) {
			Throwable cause = (Throwable) payload;
			while (cause != null) {
				for (Map.Entry<String, Class<?>> entry : this.classNameMappings.entrySet()) {
					String channelKey = entry.getKey();
					Class<?> exceptionClass = entry.getValue();
					if (exceptionClass.isInstance(cause)) {
						mostSpecificCause = channelKey;
					}
				}
				cause = cause.getCause();
			}
		}
		return Collections.singletonList(mostSpecificCause);
	}

}
