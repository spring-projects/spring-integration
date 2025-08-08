/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration;

/**
 * Indicates that a component implements some Enterprise Integration Pattern.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 *
 * @see IntegrationPatternType
 * @see <a href="https://www.enterpriseintegrationpatterns.com/patterns/messaging">EIP official site</a>
 */
public interface IntegrationPattern {

	/**
	 * Return a pattern type this component implements.
	 * @return the {@link IntegrationPatternType} this component implements.
	 */
	IntegrationPatternType getIntegrationPatternType();

}
