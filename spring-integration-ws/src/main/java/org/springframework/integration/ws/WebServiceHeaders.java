/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ws;

/**
 * Pre-defined header names to be used when storing or retrieving
 * Web Service properties to/from integration Message Headers.
 *
 * @author Mark Fisher
 */
public abstract class WebServiceHeaders {

	public static final String PREFIX = "ws_";

	public static final String SOAP_ACTION = PREFIX + "soapAction";

}
