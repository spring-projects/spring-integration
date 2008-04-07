SPRING INTEGRATION 1.0.0.M3 (Apr 07, 2008)
------------------------------------------

This is the 1.0 Milestone 3 release of Spring Integration.

To find out what has changed since Milestone 2, see 'changelog.txt'

The following are the key messaging components defined in this release:
  org.springframework.integration.message.Message
  org.springframework.integration.channel.MessageChannel
  org.springframework.integration.endpoint.MessageEndpoint
  org.springframework.integration.handler.MessageHandler
  org.springframework.integration.bus.MessageBus

The following adapters are also available in this release:
  org.springframework.integration.adapter.event.ApplicationEventSourceAdapter
  org.springframework.integration.adapter.event.ApplicationEventTargetAdapter
  org.springframework.integration.adapter.file.FileSourceAdapter
  org.springframework.integration.adapter.file.FileTargetAdapter
  org.springframework.integration.adapter.ftp.FtpSourceAdapter
  org.springframework.integration.adapter.jms.JmsMessageDrivenSourceAdapter
  org.springframework.integration.adapter.jms.JmsPollingSourceAdapter
  org.springframework.integration.adapter.jms.JmsTargetAdapter
  org.springframework.integration.adapter.mail.MailTargetAdapter
  org.springframework.integration.adapter.rmi.RmiSourceAdapter
  org.springframework.integration.adapter.rmi.RmiTargetAdapter
  org.springframework.integration.adapter.stream.ByteStreamSourceAdapter
  org.springframework.integration.adapter.stream.ByteStreamTargetAdapter
  org.springframework.integration.adapter.stream.CharacterStreamSourceAdapter
  org.springframework.integration.adapter.stream.CharacterStreamTargetAdapter
  org.springframework.integration.ws.adapter.SimpleWebServiceTargetAdapter
  org.springframework.integration.ws.adapter.MarshallingWebServiceTargetAdapter

Many of these components and adapters may be configured with either XML namespace
support or annotations. Some additional components may be configured with annotations,
such as: @Router, @Splitter, @Aggregator, @Publisher, and @Subscriber.

Please consult the documentation located within the 'docs/reference' directory of this
release for more information and also visit the official Spring Integration home at:
http://www.springframework.org/spring-integration
