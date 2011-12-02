package org.springframework.integration.flow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.flow.config.FlowUtils;
import org.springframework.integration.message.GenericMessage;
/**
 * 
 * @author David Turanski
 *
 */
public class FlowUtilsTests {
    @Test
    public void buildBridge(){
         
        SubscribableChannel inputChannel = new DirectChannel();
        SubscribableChannel outputChannel = new PublishSubscribeChannel();
        PollableChannel receiveChannel = new QueueChannel();
        
        FlowUtils.bridgeChannels(inputChannel, outputChannel);
        FlowUtils.bridgeChannels(outputChannel, receiveChannel);
      
        Message<?> message = new GenericMessage<String>("hello");
               
        inputChannel.send(message);
        Message<?> result = receiveChannel.receive(100);
        assertNotNull(result);
        assertSame(message, result);
       
    }

}
