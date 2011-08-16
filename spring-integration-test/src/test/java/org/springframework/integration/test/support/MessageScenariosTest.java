package org.springframework.integration.test.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;

@ContextConfiguration 
public class MessageScenariosTest extends AbstractRequestResponseScenarioTest {
   
    @Override
    protected List<RequestResponseScenario> defineRequestResponseScenarios() {
        List<RequestResponseScenario> scenarios= new ArrayList<RequestResponseScenario>();
        RequestResponseScenario scenario1 = new RequestResponseScenario(
                "inputChannel","outputChannel")
            .setPayload("hello")
            .setResponseValidator(new PayloadValidator() {    
                @Override
                protected void validateResponse(Object response) {
                    assertEquals("HELLO",response);
                }
            });
        
        scenarios.add(scenario1);   
        
        RequestResponseScenario scenario2 = new RequestResponseScenario(
                "inputChannel","outputChannel")
        .setMessage(MessageBuilder.withPayload("hello").setHeader("foo", "bar").build())
        .setResponseValidator(new MessageValidator() {
            @Override
            protected void validateMessage(Message<?> message) {
               assertThat(message,hasPayload("HELLO"));
               assertThat(message,hasHeader("foo","bar"));
            }    
        });
    
        scenarios.add(scenario2);   
        
        RequestResponseScenario scenario3 = new RequestResponseScenario(
                "inputChannel2","outputChannel2")
        .setMessage(MessageBuilder.withPayload("hello").setHeader("foo", "bar").build())
        .setResponseValidator(new MessageValidator() {
            @Override
            protected void validateMessage(Message<?> message) {
                assertThat(message,hasPayload("HELLO"));
                assertThat(message,hasHeader("foo","bar"));
            }    
        });
        
        scenarios.add(scenario3); 
        
        return scenarios;
    }
}
