/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.channel;

import org.springframework.integration.MessageSource;
import org.springframework.integration.MessageTarget;

/**
 * Base channel interface to combine the definitions of {@link MessageSource}
 * for message reception and and {@link MessageTarget} for message sending.
 * 
 * @author Mark Fisher
 * @see MessageSource
 * @see MessageTarget
 */
public interface MessageChannel extends MessageSource, MessageTarget {

}
