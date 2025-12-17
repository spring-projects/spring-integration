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

package org.springframework.integration.grpc;

/**
 * Constants for gRPC-specific message headers.
 *
 * @author Artem Bilan
 *
 * @since 7.1
 */
public final class GrpcHeaders {

	/**
	 * The prefix for all gRPC-specific headers.
	 */
	public static final String PREFIX = "grpc_";

	/**
	 * The header containing the called gRPC service name.
	 */
	public static final String SERVICE = PREFIX + "service";

	/**
	 * The header containing the gRPC service method name.
	 */
	public static final String SERVICE_METHOD = PREFIX + "serviceMethod";

	/**
	 * The header containing the gRPC service method type.
	 * One of the {@link io.grpc.MethodDescriptor.MethodType}
	 */
	public static final String METHOD_TYPE = PREFIX + "methodType";

	/**
	 * The header containing the gRPC service method schema descriptor.
	 * A value from the {@link io.grpc.MethodDescriptor#getSchemaDescriptor()}
	 */
	public static final String SCHEMA_DESCRIPTOR = PREFIX + "schemaDescriptor";

	private GrpcHeaders() {
	}

}
