/**
 * 
 */
package org.springframework.integration.ws;

import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.mapping.RequestReplyHeaderMapper;
import org.springframework.ws.soap.SoapHeader;

/**
 * A convenience interface that extends {@link HeaderMapper}
 * but parameterized with {@link SoapHeader}.
 *
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public interface SoapHeaderMapper extends RequestReplyHeaderMapper<SoapHeader>{

}
