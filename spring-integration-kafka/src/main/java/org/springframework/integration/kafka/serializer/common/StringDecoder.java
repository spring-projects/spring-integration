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
package org.springframework.integration.kafka.serializer.common;

import java.util.Properties;

import kafka.serializer.Decoder;
import kafka.utils.VerifiableProperties;


/**
 * String Decoder for Kafka message key/value decoding.
 * The Default decoder returns the same byte array it takes in.
 *
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 */
public class StringDecoder implements Decoder<String> {

	private final kafka.serializer.StringDecoder stringDecoder;

	public StringDecoder() {
		this("UTF8");
	}

	public StringDecoder(final String encoding) {
		final Properties props = new Properties();
		props.put("serializer.encoding", encoding);
		this.stringDecoder = new kafka.serializer.StringDecoder(new VerifiableProperties(props));
	}

	@Override
	public String fromBytes(byte[] bytes) {
		return this.stringDecoder.fromBytes(bytes);
	}

}
