package org.springframework.integration.jpa.test;

import java.util.Calendar;
import java.util.Date;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.test.entity.Gender;
import org.springframework.integration.jpa.test.entity.Student;
import org.springframework.integration.scheduling.PollerMetadata;

public final class JpaTestUtils {

	public static Student getTestStudent() {
		
		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1984, 0, 31);
		
		Student student = new Student()
		.withFirstName("First Executor")
		.withLastName("Last Executor")
		.withGender(Gender.MALE)
		.withDateOfBirth(dateOfBirth.getTime())
		.withLastUpdated(new Date());
		
		return student;
	}
	
	public static SourcePollingChannelAdapter getSourcePollingChannelAdapter(MessageSource<?> adapter, 
            MessageChannel channel, 
            PollerMetadata poller, 
            GenericApplicationContext context,
            ClassLoader beanClassLoader) throws Exception {

		SourcePollingChannelAdapterFactoryBean fb = new SourcePollingChannelAdapterFactoryBean();
		fb.setSource(adapter);
		fb.setOutputChannel(channel);
		fb.setPollerMetadata(poller);
		fb.setBeanClassLoader(beanClassLoader);
		fb.setAutoStartup(false);
		fb.setBeanFactory(context.getBeanFactory());
		fb.afterPropertiesSet();
		
		return fb.getObject();
	}
}
