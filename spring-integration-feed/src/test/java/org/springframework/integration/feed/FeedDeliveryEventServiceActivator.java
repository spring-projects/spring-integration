package org.springframework.integration.feed;

import java.util.Properties;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.history.MessageHistory;
import org.springframework.stereotype.Component;

import com.sun.syndication.feed.synd.SyndEntry;

@Component
public class FeedDeliveryEventServiceActivator {

    @ServiceActivator
    public void activate(Message<SyndEntry> message) throws Exception {

    	MessageHistory history = MessageHistory.read(message);
    	for (Properties properties : history) {
			System.out.println(properties);
		}
        SyndEntry syndEntry = message.getPayload();

        System.out.println( "Publishing new SyndEntry " + syndEntry.getUri() +":"+
                syndEntry.getPublishedDate().toString()+ ":"+ syndEntry.getPublishedDate().getTime());

//        System.out.println( syndEntry.toString());
       // System.out.println("Delivery! " + ToStringBuilder.reflectionToString(evtMsg));

    }

}
