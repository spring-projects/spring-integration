package org.springframework.integration.file.monitors;

import java.io.File;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;


/**
 * Supports propagating a {@link org.springframework.integration.Message} on the receipt of a new {@link java.io.File}
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 */
public class MessageSendingFileAdditionListener extends IntegrationObjectSupport implements FileAdditionListener {
    private MessagingTemplate messagingTemplate = new MessagingTemplate();
    private MessageChannel channel;
   
    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    @Override
    protected void onInit() throws Exception {
        Assert.notNull(this.channel, "'channel' can't be null!");
    }
    public void fileAdded(File f) {
        Message<File> fileMsg = MessageBuilder.withPayload(f).build();
        this.messagingTemplate.send(fileMsg);
    }
}
