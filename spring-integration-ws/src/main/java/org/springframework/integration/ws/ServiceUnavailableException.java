/*
 * Copyright 2016-2019 the original author or authors.
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
