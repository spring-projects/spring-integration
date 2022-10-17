/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.security.channel;

import java.util.Collection;

import org.springframework.security.access.ConfigAttribute;

/**
 * Interface to encapsulate {@link ConfigAttribute}s for secured channel
 * send and receive operations.
 *
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 * @deprecated since 6.0 in favor of literally
 * {@code new AuthorizationChannelInterceptor(AuthorityAuthorizationManager.hasAnyRole())}
 */
@Deprecated(since = "6.0")
public interface ChannelAccessPolicy {

	Collection<ConfigAttribute> getConfigAttributesForSend();

	Collection<ConfigAttribute> getConfigAttributesForReceive();

}
