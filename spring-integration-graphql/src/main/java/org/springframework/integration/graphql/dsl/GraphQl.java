/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.graphql.dsl;

import org.springframework.graphql.ExecutionGraphQlService;

/**
 * Factory class for GraphQL components DSL.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class GraphQl {

	/**
	 * Create an instance of {@link GraphQlMessageHandlerSpec} for the provided {@link ExecutionGraphQlService}.
	 * @param graphQlService the {@link ExecutionGraphQlService} to use.
	 * @return the spec.
	 */
	public static GraphQlMessageHandlerSpec gateway(ExecutionGraphQlService graphQlService) {
		return new GraphQlMessageHandlerSpec(graphQlService);
	}

	private GraphQl() {
	}

}
