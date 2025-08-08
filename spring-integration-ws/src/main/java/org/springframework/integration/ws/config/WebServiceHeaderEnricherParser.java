/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ws.config;

import org.springframework.integration.config.xml.HeaderEnricherParserSupport;
import org.springframework.integration.ws.WebServiceHeaders;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class WebServiceHeaderEnricherParser extends HeaderEnricherParserSupport {

	public WebServiceHeaderEnricherParser() {
		this.addElementToHeaderMapping("soap-action", WebServiceHeaders.SOAP_ACTION);
	}

}
