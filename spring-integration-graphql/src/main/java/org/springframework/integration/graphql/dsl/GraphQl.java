/*
 * Copyright 2022 the original author or authors.
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
