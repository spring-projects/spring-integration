/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.core;

import kafka.javaapi.consumer.SimpleConsumer;

/**
 * Wraps exceptions thrown by {@link SimpleConsumer} calls.
 *
 * Because the client is originally written in Scala, checked exceptions are missing from the method signatures
 * and cannot be caught directly.
 *
 * @author Marius Bogoevici
 */
@SuppressWarnings("serial")
public class ConsumerException extends RuntimeException {

	public ConsumerException(Throwable cause) {
		super(cause);
	}

	public ConsumerException(String message) {
		super(message);
	}

}
