/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xmpp.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.message.*;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Josh Long
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class XmppHeaderEnricherParserTests {


    private static final Log logger = LogFactory.getLog(XmppHeaderEnricherParserTests.class);

    @Value("#{input}")
    private DirectChannel input;

    @Value("#{output}")
    private DirectChannel output;

    @Test
    public void to() {
        MessagingTemplate messagingTemplate = new MessagingTemplate();
        output.subscribe(new MessageHandler() {
                public void handleMessage(Message<?> message)
                    throws MessageRejectedException, MessageHandlingException,
                        MessageDeliveryException {
                    for (String h : message.getHeaders().keySet())
                         logger.debug(String.format("%s=%s (class: %s)", h, message.getHeaders().get(h), message.getHeaders().get(h).getClass().toString()));
                }
            });
        messagingTemplate.send(input, MessageBuilder.withPayload("foo").build());
    }

}
