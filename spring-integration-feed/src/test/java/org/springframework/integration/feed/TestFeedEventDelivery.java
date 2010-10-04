package org.springframework.integration.feed;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TestFeedEventDelivery {

    @Test
    public void testDeliveryOfFeed() throws Exception {
        Thread.sleep(1000 * 60);
    }

    /*  public static void main(String[] args) throws Throwable {
        String siweb = "http://twitter.com/statuses/public_timeline.atom"; //http://localhost:8080/siweb/foo.atom";
        FeedEntryReaderMessageSource feedEntryReaderMessageSource = new FeedEntryReaderMessageSource();
        feedEntryReaderMessageSource.setFeedUrl(siweb);
        feedEntryReaderMessageSource.afterPropertiesSet();
        feedEntryReaderMessageSource.start();

        while (true) {
            Message<SyndEntry> entryMessage = feedEntryReaderMessageSource.receive();

            if (entryMessage != null) {
                SyndEntry entry = entryMessage.getPayload();
                System.out.println((entry.getTitle() + "=" + entry.getUri()));
            }

            Thread.sleep(1000);
        }
    }
 */
}
