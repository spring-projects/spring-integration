/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
