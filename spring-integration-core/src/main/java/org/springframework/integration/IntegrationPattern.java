/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
