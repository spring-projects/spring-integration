package org.springframework.integration.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MethodInvokingMessageGroupProcessorTests {

    @Mock
    private BufferedMessagesCallback processedCallback;

    @Mock
    private MessageChannel outputChannel;

    private Collection<Message<?>> messagesUpForProcessing = new ArrayList<Message<?>>(
            3);

    @Before
    public void initializeMessagesUpForProcessing() {
        messagesUpForProcessing.add(MessageBuilder.withPayload(1).build());
        messagesUpForProcessing.add(MessageBuilder.withPayload(2).build());
        messagesUpForProcessing.add(MessageBuilder.withPayload(4).build());
    }

    private class AnnotatedAggregatorMethod {

        @Aggregator
        @SuppressWarnings("unused")
        public Integer and(List<Integer> flags) {
            int result = 0;
            for (Integer flag : flags) {
                result = result | flag;
            }
            return result;
        }

        public String know() {
            return "I'm not the one ";
        }
    }

    @Test
    public void shouldFindAnnotatedAggregatorMethod() throws Exception {
        MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(
                new AnnotatedAggregatorMethod());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor
                .forClass(Message.class);
        when(outputChannel.send(isA(Message.class))).thenReturn(true);
        processor.processAndSend(3, messagesUpForProcessing, outputChannel,
                processedCallback);
        // verify
        verify(outputChannel).send(messageCaptor.capture());
        assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
    }


    private class SimpleAggregator {
        public Integer and(List<Integer> flags) {
            int result = 0;
            for (Integer flag : flags) {
                result = result | flag;
            }
            return result;
        }
    }

    @Test
    public void shouldFindSimpleAggregatorMethod() throws Exception {
        MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(
                new SimpleAggregator());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor
                .forClass(Message.class);
        when(outputChannel.send(isA(Message.class))).thenReturn(true);
        processor.processAndSend(3, messagesUpForProcessing, outputChannel,
                processedCallback);
        // verify
        verify(outputChannel).send(messageCaptor.capture());
        assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
    }


    private class UnnanotatedAggregator {
        public Integer and(List<Integer> flags) {
            int result = 0;
            for (Integer flag : flags) {
                result = result | flag;
            }
            return result;
        }

        public void voidMethodShouldBeIgnored(List<Integer> flags){
            fail("this method should not be invoked");
        }
        public String methodAcceptingNoCollectionShouldBeIgnored(@Header String irrelevant){
            fail("this method should not be invoked");
            return null;
        }
    }

    @Test
    public void shouldFindFittingMethodAmongMultipleUnanotated() {
        MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(
                new UnnanotatedAggregator()
        );

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor
                .forClass(Message.class) ;

        when(outputChannel.send(isA(Message.class))).thenReturn(true);
        processor.processAndSend(3, messagesUpForProcessing, outputChannel,
                processedCallback);
        // verify
        verify(outputChannel).send(messageCaptor.capture());
        assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
    }
    private class AnnotatedParametersAggregator {
        public Integer and(List<Integer> flags) {
            int result = 0;
            for (Integer flag : flags) {
                result = result | flag;
            }
            return result;
        }

        public String listHeaderShouldBeIgnored(@Header List<Integer> flags){
            fail("this method should not be invoked");
            return "";
        }
    }

    @Test
    public void shouldFindFittingMethodAmongMultipleWithAnnotatedParameters() {
        MessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(
                new AnnotatedParametersAggregator()
        );

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor
                .forClass(Message.class) ;

        when(outputChannel.send(isA(Message.class))).thenReturn(true);
        processor.processAndSend(3, messagesUpForProcessing, outputChannel,
                processedCallback);
        // verify
        verify(outputChannel).send(messageCaptor.capture());
        assertThat((Integer) messageCaptor.getValue().getPayload(), is(7));
    }
}
