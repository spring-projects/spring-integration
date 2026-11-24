/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import org.jspecify.annotations.Nullable;

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
	public @Nullable ConversionService getObject() {
		ConversionService service = super.getObject();
		if (service instanceof GenericConversionService) {
			((GenericConversionService) service).removeConvertible(Object.class, Object.class);
		}
		return service;
	}

}
