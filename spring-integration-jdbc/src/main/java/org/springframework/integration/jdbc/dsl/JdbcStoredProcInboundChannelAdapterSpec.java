/*
 * Copyright 2025-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.inbound.StoredProcPollingChannelAdapter;
import org.springframework.util.Assert;

/**
 * A {@link MessageSourceSpec} for a {@link JdbcStoredProcInboundChannelAdapterSpec}.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class JdbcStoredProcInboundChannelAdapterSpec
		extends MessageSourceSpec<JdbcStoredProcInboundChannelAdapterSpec, StoredProcPollingChannelAdapter>
		implements ComponentsRegistration {

	private final StoredProcExecutor storedProcExecutor;

	private @Nullable StoredProcExecutorSpec storedProcExecutorSpec;

	protected JdbcStoredProcInboundChannelAdapterSpec(StoredProcExecutorSpec storedProcExecutorSpec) {
		this(storedProcExecutorSpec.getObject());
		this.storedProcExecutorSpec = storedProcExecutorSpec;
	}

	protected JdbcStoredProcInboundChannelAdapterSpec(StoredProcExecutor storedProcExecutor) {
		this.storedProcExecutor = storedProcExecutor;
		this.storedProcExecutorSpec = null;
		this.target = new StoredProcPollingChannelAdapter(this.storedProcExecutor);
	}

	/**
	 * Configure the storedProcExecutor through storedProcExecutorConfigurer by invoking the {@link Consumer} callback
	 * @param configurer the configurer.
	 * @return the spec
	 */
	public JdbcStoredProcInboundChannelAdapterSpec configurerStoredProcExecutor(
			Consumer<StoredProcExecutorSpec> configurer) {

		Assert.notNull(configurer, "'configurer' must not be null");
		Assert.notNull(this.storedProcExecutorSpec,
				"The externally provided 'StoredProcExecutor' cannot be mutated in this spec");
		configurer.accept(this.storedProcExecutorSpec);
		return this;
	}

	/**
	 * @param expectSingleResult the expectSingleResult
	 * @return the spec
	 * @see StoredProcPollingChannelAdapter#setExpectSingleResult(boolean)
	 */
	public JdbcStoredProcInboundChannelAdapterSpec expectSingleResult(boolean expectSingleResult) {
		this.target.setExpectSingleResult(expectSingleResult);
		return this;
	}

	@Override
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.<Object, @Nullable String>singletonMap(this.storedProcExecutor, null);
	}

}
