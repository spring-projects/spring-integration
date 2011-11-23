package org.springframework.integration.test.support;

import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.Assert;
/**
 * Defines a Spring Integration request response test scenario. All setter methods may
 * be chained. 
 * @author David Turanski
 *
 */
public class RequestResponseScenario {
    private final String inputChannelName;
    private final String outputChannelName;
    private Object payload;
    private Message<?> message;
    private AbstractResponseValidator<?> responseValidator;
    private String name;
 
    protected Message<? extends Object> getMessage(){
        if (message == null){
            return new GenericMessage<Object>(this.payload);
        } else {
            return message;
        }
    }
    /**
     * Create an instance
     * @param inputChannelName the input channel name
     * @param outputChannelName the output channel name
     */
    public RequestResponseScenario(String inputChannelName, String outputChannelName){
        this.inputChannelName = inputChannelName;
        this.outputChannelName = outputChannelName;
    }
    
    /**
     * 
     * @return the input channel name
     */
    public String getInputChannelName() {
        return inputChannelName;
    }
    
    /**
     * 
     * @return the output channel name
     */
    public String getOutputChannelName() {
        return outputChannelName;
    }
    
    /**
     * 
     * @return the request message payload
     */
    public Object getPayload() {
        return payload;
    }
    
    /**
     * set the payload of the request message
     * @param payload
     * @return this
     */
    public RequestResponseScenario setPayload(Object payload) {
        this.payload = payload;
        return this;
    }
    
    /**
     * 
     * @return the scenario name
     */
    public String getName() {
        return name;
    }
    
    /**
     * set the scenario name (optional)
     * @param name the name
     * @return this
     */
    public RequestResponseScenario setName(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 
     * @return the response validator 
     * @see AbstractResponseValidator
     */
    public AbstractResponseValidator<?> getResponseValidator(){
        return responseValidator;
    }
    
    /**
     * Set the response validator 
     * @see AbstractResponseValidator
     * @param responseValidator
     * @return this
     */
    public RequestResponseScenario setResponseValidator(AbstractResponseValidator<?> responseValidator) {
        this.responseValidator = responseValidator;
        return this;
    }
    
   
    /**
     * Set the request message (as an alternative to setPayload())
     * @param message
     * @return this
     */
    public RequestResponseScenario setMessage(Message<?> message) {
        this.message = message;
        return this;
    }

    protected void init() {
        Assert.state(message == null || payload == null,"cannot set both message and payload");
    }

}
