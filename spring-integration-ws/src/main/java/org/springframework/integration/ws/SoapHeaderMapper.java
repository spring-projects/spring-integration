/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws;

import org.springframework.integration.mapping.RequestReplyHeaderMapper;
import org.springframework.ws.soap.SoapMessage;

/**
 * A convenience interface that extends {@link RequestReplyHeaderMapper},
 * parameterized with {@link org.springframework.ws.soap.SoapHeader}.
 *
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public interface SoapHeaderMapper extends RequestReplyHeaderMapper<SoapMessage> {

}
