/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.serializer.common;

import java.util.Properties;

import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

/**
 * @author Soby Chacko
 * @author Marius Bogoevici
 * @since 0.5
 */
public class StringEncoder implements Encoder<String> {

	private kafka.serializer.StringEncoder stringEncoder;

	public StringEncoder() {
		this("UTF-8");
	}

	public StringEncoder(String encoding) {
		final Properties props = new Properties();
		props.put("serializer.encoding", encoding);
		final VerifiableProperties verifiableProperties = new VerifiableProperties(props);
		stringEncoder = new kafka.serializer.StringEncoder(verifiableProperties);
	}

	@Override
	public byte[] toBytes(final String value) {
		return stringEncoder.toBytes(value);
	}
}
