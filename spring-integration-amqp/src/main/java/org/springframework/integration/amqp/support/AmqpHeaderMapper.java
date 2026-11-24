/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.amqp.support;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.integration.mapping.RequestReplyHeaderMapper;

/**
 * A convenience interface that extends {@link RequestReplyHeaderMapper},
 * parameterized with {@link MessageProperties}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public interface AmqpHeaderMapper extends RequestReplyHeaderMapper<MessageProperties> {

}
