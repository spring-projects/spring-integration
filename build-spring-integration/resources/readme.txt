SPRING INTEGRATION 1.0 M1 (JAN 23 2008)
---------------------------------------

This is the initial release of Spring Integration.

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
  org.springframework.integration.adapter.jms.JmsMessageDrivenSourceAdapter
  org.springframework.integration.adapter.jms.JmsPollingSourceAdapter
  org.springframework.integration.adapter.jms.JmsTargetAdapter
  org.springframework.integration.adapter.stream.ByteStreamSourceAdapter
  org.springframework.integration.adapter.stream.ByteStreamTargetAdapter
  org.springframework.integration.adapter.stream.CharacterStreamSourceAdapter
  org.springframework.integration.adapter.stream.CharacterStreamTargetAdapter

Many of these components and adapters may be configured with either XML namespace
support or annotations. Some additional components may be configured with annotations,
such as: @Router, @Splitter, @Publisher, and @Subscriber.

Please consult the documentation located within the 'docs/reference' directory of this
release for more information and also visit the official Spring Integration home at:
http://www.springframework.org/spring-integration
