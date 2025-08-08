/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.support.management.micrometer;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * An {@link ImportSelector} to conditionally add a {@link MicrometerMetricsCaptorConfiguration}
 * bean when {@code io.micrometer.core.instrument.MeterRegistry} is present in classpath.
 *
 * @author Artem Bilan
 *
 * @since 5.5.5
 */
public class MicrometerMetricsCaptorImportSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		if (MicrometerMetricsCaptorConfiguration.METER_REGISTRY_PRESENT) {
			return new String[] {MicrometerMetricsCaptorConfiguration.class.getName()};
		}
		else {
			return new String[0];
		}
	}

}
