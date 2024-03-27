/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.xmpp.support;

import org.jivesoftware.smack.packet.MessageBuilder;

import org.springframework.integration.mapping.RequestReplyHeaderMapper;

/**
 * A convenience interface that extends {@link RequestReplyHeaderMapper}
 * but parameterized with the Smack API {@link MessageBuilder}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Florian Schmaus
 *
 * @since 2.1
 */
public interface XmppHeaderMapper extends RequestReplyHeaderMapper<MessageBuilder> {

}
