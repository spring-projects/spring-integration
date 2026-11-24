/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config.xml;

import org.springframework.integration.config.TransformerFactoryBean;

/**
 * Parser for the &lt;transformer/&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class TransformerParser extends AbstractDelegatingConsumerEndpointParser {

	@Override
	String getFactoryBeanClassName() {
		return TransformerFactoryBean.class.getName();
	}

	@Override
	boolean hasDefaultOption() {
		return false;
	}

}
