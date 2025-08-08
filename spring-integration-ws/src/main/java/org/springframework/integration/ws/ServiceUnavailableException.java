/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ws;

import org.springframework.ws.WebServiceException;
import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * The {@link WebServiceException} extension to indicate that the server endpoint is
 * temporary unavailable.
 *
 * @author Artem Bilan
 * @since 4.3
 */
@SoapFault(faultCode = FaultCode.RECEIVER)
public class ServiceUnavailableException extends WebServiceException {

	private static final long serialVersionUID = 1L;

	public ServiceUnavailableException(String msg) {
		super(msg);
	}

	public ServiceUnavailableException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
