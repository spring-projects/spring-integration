/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * This is a workaround until SPR-8818 will be resolved.
 * See INT-2259 and INT-1893 for more detail.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
class CustomConversionServiceFactoryBean extends ConversionServiceFactoryBean {

	@Override
	public ConversionService getObject() {
		ConversionService service = super.getObject();
		if (service instanceof GenericConversionService) {
			((GenericConversionService) service).removeConvertible(Object.class, Object.class);
		}
		return service;
	}

}
