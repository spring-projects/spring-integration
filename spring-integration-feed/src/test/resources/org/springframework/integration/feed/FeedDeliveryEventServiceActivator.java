package org.springframework.integration.feed;

import com.sun.syndication.feed.synd.SyndEntry;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

@Component
public class FeedDeliveryEventServiceActivator {

    @ServiceActivator
    public void activate(Message<SyndEntry> evtMsg) throws Exception {

        SyndEntry syndEntry = evtMsg.getPayload();

        System.out.println( "Publishing new SyndEntry " + syndEntry.getUri() +":"+
                syndEntry.getPublishedDate().toString()+ ":"+ syndEntry.getPublishedDate().getTime());

//        System.out.println( syndEntry.toString());
       // System.out.println("Delivery! " + ToStringBuilder.reflectionToString(evtMsg));

    }

}
