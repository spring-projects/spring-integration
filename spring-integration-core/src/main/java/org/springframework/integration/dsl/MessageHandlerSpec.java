/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.messaging.MessageHandler;

/**
 * An {@link IntegrationComponentSpec} for {@link MessageHandler}s.
 *
 * @param <S> the target {@link ConsumerEndpointSpec} implementation type.
 * @param <H> the target {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class MessageHandlerSpec<S extends MessageHandlerSpec<S, H>, H extends MessageHandler>
		extends IntegrationComponentSpec<S, H> {

}
