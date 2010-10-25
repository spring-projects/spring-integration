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
package org.springframework.integration.twitter.inbound;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;
import org.springframework.util.Assert;

import twitter4j.Twitter;


/**
 * Abstract class that defines common operations for receiving various types of messages when using the
 * Twitter API. 
 * This class also handles keeping track on the latest inbound message its received and avoiding, where
 * possible, redelivery of common messages. This functionality is enabled using the
 * {@link org.springframework.integration.context.metadata.MetadataStore} implementation
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * 
 * @since 2.0
 */
public abstract class AbstractInboundTwitterEndpointSupport<T> extends MessageProducerSupport implements Lifecycle, TrackableComponent{
	
    protected volatile OAuthConfiguration configuration;
    protected volatile long markerId = -1;
    protected Twitter twitter;
    private final Object markerGuard = new Object();
    private volatile ScheduledFuture<?> twitterUpdatePollingTask;

    private final HistoryWritingMessagePostProcessor historyWritingPostProcessor = new HistoryWritingMessagePostProcessor();

    abstract protected void markLastStatusId(T statusId);

    abstract protected List<T> sort(List<T> rl);
    
    abstract Runnable getApiCallback();
    
    public void setConfiguration(OAuthConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public void setShouldTrack(boolean shouldTrack) {
        this.historyWritingPostProcessor.setShouldTrack(shouldTrack);
    }
    
    public long getMarkerId() {
        return markerId;
    }

    @Override
    protected void onInit() {
        super.onInit();
        Assert.notNull(this.configuration, "'configuration' can't be null");
        this.twitter = this.configuration.getTwitter();
        Assert.notNull(this.twitter, "'twitter' instance can't be null");
    }

    protected void forwardAll(List<T> tResponses) {
        List<T> stats = new ArrayList<T>();

        for (T t : tResponses){
        	stats.add(t);
        }
           
        for (T twitterResponse : sort(stats)) {
        	 forward(twitterResponse);
        }         
    }

    @Override
    protected void doStart() {
        historyWritingPostProcessor.setTrackableComponent(this);  
    	RateLimitStatusTrigger trigger = new RateLimitStatusTrigger(this.twitter); 	
    	Runnable apiCallback = this.getApiCallback();
    	twitterUpdatePollingTask = this.getTaskScheduler().schedule(apiCallback, trigger);
    }

    @Override
    protected void doStop() {
    	twitterUpdatePollingTask.cancel(true);
    }
    
    protected void forward(T status) {
        synchronized (this.markerGuard) {
            Message<T> twtMsg = MessageBuilder.withPayload(status).build();

            sendMessage(twtMsg);

            markLastStatusId(status);
        }
    }
    protected boolean hasMarkedStatus() {
        return markerId > -1;
    }
}
