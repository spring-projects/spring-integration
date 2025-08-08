/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.0.2
 */
public abstract class HttpHeaders {

	public static final String PREFIX = "http_";

	public static final String REQUEST_URL = PREFIX + "requestUrl";

	public static final String REQUEST_METHOD = PREFIX + "requestMethod";

	public static final String USER_PRINCIPAL = PREFIX + "userPrincipal";

	public static final String STATUS_CODE = PREFIX + "statusCode";

}
