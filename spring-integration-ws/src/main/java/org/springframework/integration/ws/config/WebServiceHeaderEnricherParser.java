/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import org.springframework.integration.config.xml.HeaderEnricherParserSupport;
import org.springframework.integration.ws.WebServiceHeaders;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class WebServiceHeaderEnricherParser extends HeaderEnricherParserSupport {

	@SuppressWarnings("this-escape")
	public WebServiceHeaderEnricherParser() {
		this.addElementToHeaderMapping("soap-action", WebServiceHeaders.SOAP_ACTION);
	}

}
