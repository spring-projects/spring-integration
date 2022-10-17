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

package org.springframework.integration.security.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the security namespace.
 *
 * @author Jonas Partner
 *
 * @deprecated since 6.0 in favor of literally
 * {@code new AuthorizationChannelInterceptor(AuthorityAuthorizationManager.hasAnyRole())}
 */
@Deprecated(since = "6.0", forRemoval = true)
public class IntegrationSecurityNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("secured-channels", new SecuredChannelsParser());
	}

}
