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

package org.springframework.integration.amqp;

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving AMQP
 * MessageProperties from/to integration Message Headers.
 * @deprecated in favor of {@link org.springframework.amqp.support.AmqpHeaders}.
 * Will be removed in a future release.
 *
 * @author Mark Fisher
 */
@Deprecated
public abstract class AmqpHeaders extends org.springframework.amqp.support.AmqpHeaders {
}
