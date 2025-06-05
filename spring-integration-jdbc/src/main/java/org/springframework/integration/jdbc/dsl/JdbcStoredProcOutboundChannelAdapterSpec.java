/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.integration.jdbc.dsl;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.StoredProcMessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for a {@link JdbcStoredProcOutboundChannelAdapterSpec}.
 *
 * @author Jiandong Ma
 *
 * @since 7.0
 */
public class JdbcStoredProcOutboundChannelAdapterSpec
		extends MessageHandlerSpec<JdbcStoredProcOutboundChannelAdapterSpec, StoredProcMessageHandler>
		implements ComponentsRegistration {

	private final StoredProcExecutor storedProcExecutor;

	private final StoredProcExecutorConfigurer storedProcExecutorConfigurer;

	protected JdbcStoredProcOutboundChannelAdapterSpec(StoredProcExecutor storedProcExecutor) {
		this.storedProcExecutor = storedProcExecutor;
		this.storedProcExecutorConfigurer = new StoredProcExecutorConfigurer(this.storedProcExecutor);
		this.target = new StoredProcMessageHandler(this.storedProcExecutor);
	}

	/**
	 * Configure the storedProcExecutor through storedProcExecutorConfigurer by invoking the {@link Consumer} callback
	 * @param configurer the configurer.
	 * @return the spec
	 */
	public JdbcStoredProcOutboundChannelAdapterSpec configurerStoredProcExecutor(Consumer<StoredProcExecutorConfigurer> configurer) {
		Assert.notNull(configurer, "'configurer' must not be null");
		configurer.accept(this.storedProcExecutorConfigurer);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(this.storedProcExecutor, null);
	}
}
