/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.jdbc.dsl;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.outbound.StoredProcOutboundGateway;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for a {@link JdbcStoredProcOutboundGatewaySpec}.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class JdbcStoredProcOutboundGatewaySpec
		extends MessageHandlerSpec<JdbcStoredProcOutboundGatewaySpec, StoredProcOutboundGateway>
		implements ComponentsRegistration {

	private final StoredProcExecutor storedProcExecutor;

	private @Nullable StoredProcExecutorSpec storedProcExecutorSpec;

	protected JdbcStoredProcOutboundGatewaySpec(StoredProcExecutorSpec storedProcExecutorSpec) {
		this(storedProcExecutorSpec.getObject());
		this.storedProcExecutorSpec = storedProcExecutorSpec;
	}

	protected JdbcStoredProcOutboundGatewaySpec(StoredProcExecutor storedProcExecutor) {
		this.storedProcExecutor = storedProcExecutor;
		this.storedProcExecutorSpec = null;
		this.target = new StoredProcOutboundGateway(this.storedProcExecutor);
	}

	/**
	 * Configure the storedProcExecutor through storedProcExecutorConfigurer by invoking the {@link Consumer} callback
	 * @param configurer the configurer.
	 * @return the spec
	 */
	public JdbcStoredProcOutboundGatewaySpec configurerStoredProcExecutor(Consumer<StoredProcExecutorSpec> configurer) {
		Assert.notNull(configurer, "'configurer' must not be null");
		Assert.notNull(this.storedProcExecutorSpec,
				"The externally provided 'StoredProcExecutor' cannot be mutated in this spec");
		configurer.accept(this.storedProcExecutorSpec);
		return this;
	}

	/**
	 * @param requiresReply the requiresReply
	 * @return the spec
	 * @see StoredProcOutboundGateway#setRequiresReply(boolean)
	 */
	public JdbcStoredProcOutboundGatewaySpec requiresReply(boolean requiresReply) {
		this.target.setRequiresReply(requiresReply);
		return this;
	}

	/**
	 * @param expectSingleResult the expectSingleResult
	 * @return the spec
	 * @see StoredProcOutboundGateway#setExpectSingleResult(boolean)
	 */
	public JdbcStoredProcOutboundGatewaySpec expectSingleResult(boolean expectSingleResult) {
		this.target.setExpectSingleResult(expectSingleResult);
		return this;
	}

	@Override
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.<Object, @Nullable String>singletonMap(this.storedProcExecutor, null);
	}

}
