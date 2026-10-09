/*
 * Copyright 2025-present the original author or authors.
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
