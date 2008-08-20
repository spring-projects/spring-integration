SPRING INTEGRATION 1.0.0.M6 (Aug 20, 2008)
------------------------------------------

This is the 1.0 Milestone 6 release of Spring Integration.

To find out what has changed since Milestone 5, see 'changelog.txt'

The following are the key messaging components defined in this release:
  org.springframework.integration.message.Message
  org.springframework.integration.channel.MessageChannel
  org.springframework.integration.endpoint.MessageEndpoint
  org.springframework.integration.message.MessageSource
  org.springframework.integration.message.MessageTarget
  org.springframework.integration.handler.MessageHandler
  org.springframework.integration.transformer.MessageTransformer
  org.springframework.integration.bus.MessageBus

The following adapters are also available in this release:
  org.springframework.integration.adapter.event.ApplicationEventSource
  org.springframework.integration.adapter.event.ApplicationEventTarget
  org.springframework.integration.adapter.file.FileSource
  org.springframework.integration.adapter.file.FileTarget
  org.springframework.integration.adapter.ftp.FtpSource
  org.springframework.integration.adapter.ftp.FtpTarget
  org.springframework.integration.adapter.httpinvoker.HttpInvokerGateway
  org.springframework.integration.adapter.httpinvoker.HttpInvokerHandler
  org.springframework.integration.adapter.jms.JmsGateway
  org.springframework.integration.adapter.jms.JmsSource
  org.springframework.integration.adapter.jms.JmsTarget
  org.springframework.integration.adapter.mail.MailTarget
  org.springframework.integration.adapter.mail.PollingMailSource
  org.springframework.integration.adapter.mail.SubscribableMailSource
  org.springframework.integration.adapter.rmi.RmiGateway
  org.springframework.integration.adapter.rmi.RmiHandler
  org.springframework.integration.adapter.stream.ByteStreamSource
  org.springframework.integration.adapter.stream.ByteStreamTarget
  org.springframework.integration.adapter.stream.CharacterStreamSource
  org.springframework.integration.adapter.stream.CharacterStreamTarget
  org.springframework.integration.ws.handler.SimpleWebServiceHandler
  org.springframework.integration.ws.handler.MarshallingWebServiceHandler

Many of these components and adapters may be configured with either XML namespace
support or annotations. Routers, Splitters, and Aggregators may also be configured
with either XML namespace support or annotations.

Please consult the documentation located within the 'docs/reference' directory of this
release for more information and also visit the official Spring Integration home at:
http://www.springframework.org/spring-integration

To checkout the project from the SVN head and build from source, do the following:

    svn co https://src.springframework.org/svn/spring-integration/trunk .
    cd build-spring-integration
    ant jar test package

The result is available as a zip file in "build-spring-integration/target/artifacts"
An expanded version is also available in "build-spring-integration/target/package-expanded"

To run the code within Eclipse, do the following:

   Import... > General > Existing Projects into Workspace
   Browse to the directory where you checked out the project
   Select each module that begins with "org.springframework.integration"
   Define a Classpath Variable named IVY_CACHE under "Preferences > Java > Build Path"
   Its value should be: <checkout-dir>/ivy-cache/repository

To run the code within Idea, do the following:

   Import the existing Eclipse projects (File > New Project... > Import project from external model > Eclipse)
   Define a Path Variable named IVY_CACHE (IDE Settings > Path Variables)
   Its value should be: <checkout-dir>/ivy-cache/repository
