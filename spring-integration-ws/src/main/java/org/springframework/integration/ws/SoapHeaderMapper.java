/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
