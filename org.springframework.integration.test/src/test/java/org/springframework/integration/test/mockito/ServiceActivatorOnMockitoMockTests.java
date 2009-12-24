package org.springframework.integration.test.mockito;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore //remove to reproduce INT-944
public class ServiceActivatorOnMockitoMockTests {

    @Autowired @Qualifier("in")
    MessageChannel in;

    @Autowired @Qualifier("out")
    PollableChannel out;

    public static interface SingleMethod {
        String move(String s);
    }

    @Test
    public void shouldInvokeMock() {
        in.send(MessageBuilder.withPayload("test").build())    ;
    }

}
