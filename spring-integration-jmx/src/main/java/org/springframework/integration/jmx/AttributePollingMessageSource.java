/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jmx;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation that
 * retrieves the current value of a JMX attribute each time {@link #receive()} is invoked.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.inbound.AttributePollingMessageSource}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class AttributePollingMessageSource
		extends org.springframework.integration.jmx.inbound.AttributePollingMessageSource {

}
