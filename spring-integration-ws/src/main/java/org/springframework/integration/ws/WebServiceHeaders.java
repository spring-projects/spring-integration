/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
