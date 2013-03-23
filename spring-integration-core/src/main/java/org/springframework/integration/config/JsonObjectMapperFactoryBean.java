package org.springframework.integration.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.json.JacksonJsonObjectMapper;
import org.springframework.integration.json.JsonObjectMapper;
import org.springframework.util.Assert;

/**
 * Wrapper {@link FactoryBean} to perform check on the <code>object-mapper</code> attribute bean reference type.
 * In addition says a 'warn' message about deprecation of usage of Jackson's #{link ObjectMapper}
 * as bean reference for &lt;json-to-object-transformer&gt; and &lt;object-to-json-transformer&gt;.
 * This class is @deprecated in favor to use {@link JsonObjectMapper} strategy implementations as bean reference
 * for <code>&lt;json-to-object-transformer&gt;</code> and <code>&lt;object-to-json-transformer&gt;</code>.
 *
 * @author Artem Bilan
 * @since 3.0
 */
@Deprecated
class JsonObjectMapperFactoryBean implements FactoryBean<JsonObjectMapper>, InitializingBean, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private JsonObjectMapper objectMapper;

	private String objectMapperBeanName;

	private BeanFactory beanFactory;

	private boolean isJacksonObjectMapper;

	public JsonObjectMapperFactoryBean(String objectMapperBeanName) {
		this.objectMapperBeanName = objectMapperBeanName;
	}

	@Override
	public JsonObjectMapper getObject() throws Exception {
		Assert.notNull(this.objectMapper, "Incompatible bean reference '" + this.objectMapperBeanName + "'. " +
				"Has to be an instance of 'org.springframework.integration.json.JsonObjectMapper' " +
				"or 'org.codehaus.jackson.map.ObjectMapper'");
		if (this.isJacksonObjectMapper) {
			logger.warn("Jackson's 'org.codehaus.jackson.map.ObjectMapper' bean reference is DEPRECATED " +
					"and will be removed in future releases. " +
					"Please, use 'org.springframework.integration.json.JsonObjectMapper' abstraction implementation reference instead.");
		}
		return this.objectMapper;
	}

	@Override
	public Class<JsonObjectMapper> getObjectType() {
		return JsonObjectMapper.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Object objectMapperBean = this.beanFactory.getBean(this.objectMapperBeanName);
		if (objectMapperBean instanceof JsonObjectMapper) {
			this.objectMapper = (JsonObjectMapper) objectMapperBean;
		}
		else if (objectMapperBean instanceof ObjectMapper) {
			this.objectMapper = new JacksonJsonObjectMapper((ObjectMapper) objectMapperBean);
			this.isJacksonObjectMapper = true;
		}
	}

}
