/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

/**
 * An outbound Messaging Gateway for request/reply JMS.
 *
 * @author Mark Fisher
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.outbound.JmsOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JmsOutboundGateway extends org.springframework.integration.jms.outbound.JmsOutboundGateway {

}
