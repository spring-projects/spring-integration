/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support.converter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.integration.support.json.JacksonPresent;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * A {@link CompositeMessageConverter} extension with some default {@link MessageConverter}s
 * which can be overridden with the given converters
 * or added in the end of target {@code converters} collection.
 * <p>
 * The default converts are (declared exactly in this order):
 * <ul>
 *  <li> {@link MappingJackson2MessageConverter} if Jackson processor is present in classpath;
 *  <li> {@link ByteArrayMessageConverter}
 *  <li> {@link ObjectStringMessageConverter}
 *  <li> {@link GenericMessageConverter}
 * </ul>
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ConfigurableCompositeMessageConverter extends CompositeMessageConverter {

	/**
	 * Create an instance with the default converters.
	 */
	public ConfigurableCompositeMessageConverter() {
		super(initDefaults());
	}

	/**
	 * Create an instance with the given converters and without defaults.
	 * @param converters the converters to use
	 */
	public ConfigurableCompositeMessageConverter(Collection<MessageConverter> converters) {
		this(converters, false);
	}

	/**
	 * Create an instance with the given converters and with defaults in the end.
	 * @param converters the converters to use
	 * @param registerDefaults register or not default converts
	 */
	public ConfigurableCompositeMessageConverter(Collection<MessageConverter> converters, boolean registerDefaults) {
		super(registerDefaults ?
				Stream.concat(converters.stream(), initDefaults().stream()).collect(Collectors.toList())
				: converters);
	}

	private static Collection<MessageConverter> initDefaults() {
		List<MessageConverter> converters = new LinkedList<>();

		if (JacksonPresent.isJackson2Present()) {
			converters.add(new MappingJackson2MessageConverter());
		}
		converters.add(new ByteArrayMessageConverter());
		converters.add(new ObjectStringMessageConverter());

		// TODO do we port it together with MessageConverterUtils ?
		// converters.add(new JavaSerializationMessageConverter());

		converters.add(new GenericMessageConverter());

		return converters;
	}

}
