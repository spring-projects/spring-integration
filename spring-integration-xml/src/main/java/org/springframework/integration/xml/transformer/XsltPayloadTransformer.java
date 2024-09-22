/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.transformer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.VfsResource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerFactoryUtils;

/**
 * Thread safe XSLT transformer implementation which returns a transformed
 * {@link Source}, {@link Document}, or {@link String}. If
 * alwaysUseSourceResultFactories is false (default) the following logic occurs
 * <p>
 * {@link String} payload in results in {@link String} payload out
 * <p>
 * {@link Document} payload in results in {@link Document} payload out
 * <p>
 * {@link Source} payload in results in {@link Result} payload out, type will be
 * determined by the {@link ResultFactory},
 * {@link org.springframework.integration.xml.result.DomResultFactory} by default.
 * If an instance of {@link ResultTransformer} is registered this will be used
 * to convert the result.
 * <p>
 * If alwaysUseSourceResultFactories is true then the ResultFactory and
 * {@link SourceFactory} will be used to create the {@link Source} from the
 * payload and the {@link Result} to pass into the transformer. An instance of
 * {@link ResultTransformer} can also be provided to convert the Result prior to
 * returning.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Mike Bazos
 * @author Gary Russell
 * @author Trung Pham
 * @author Ngoc Nhan
 */
public class XsltPayloadTransformer extends AbstractXmlTransformer implements BeanClassLoaderAware {

	private final ResultTransformer resultTransformer;

	private final Resource xslResource;

	private Templates templates;

	private String transformerFactoryClassName;

	private volatile StandardEvaluationContext evaluationContext;

	private Map<String, Expression> xslParameterMappings;

	private SourceFactory sourceFactory = new DomSourceFactory();

	private boolean resultFactoryExplicitlySet;

	private boolean alwaysUseSourceFactory = false;

	private boolean alwaysUseResultFactory = false;

	private String[] xsltParamHeaders;

	public XsltPayloadTransformer(Templates templates) {
		this(templates, null);
	}

	private ClassLoader classLoader;

	public XsltPayloadTransformer(Templates templates, ResultTransformer resultTransformer) {
		Assert.notNull(templates, "'templates' must not be null.");
		this.templates = templates;
		this.resultTransformer = resultTransformer;
		this.xslResource = null;
	}

	public XsltPayloadTransformer(Resource xslResource) {
		this(xslResource, null, null);
	}

	public XsltPayloadTransformer(Resource xslResource, ResultTransformer resultTransformer) {
		this(xslResource, resultTransformer, null);
	}

	public XsltPayloadTransformer(Resource xslResource, String transformerFactoryClassName) {
		this(xslResource, null, transformerFactoryClassName);
	}

	public XsltPayloadTransformer(Resource xslResource, ResultTransformer resultTransformer,
			String transformerFactoryClassName) {

		Assert.notNull(xslResource, "'xslResource' must not be null.");
		Assert.isTrue(xslResource instanceof ClassPathResource ||
						xslResource instanceof FileSystemResource ||
						xslResource instanceof VfsResource || // NOSONAR boolean complexity
						xslResource.getClass().getName()
								.equals("org.springframework.web.context.support.ServletContextResource"),
				"Only 'ClassPathResource', 'FileSystemResource', 'ServletContextResource' or 'VfsResource'" +
						" are supported directly in this transformer. For any other 'Resource' implementations" +
						" consider to use a 'Templates'-based constructor instantiation.");
		this.xslResource = xslResource;
		this.resultTransformer = resultTransformer;
		this.transformerFactoryClassName = transformerFactoryClassName;
	}

	/**
	 * Sets the SourceFactory.
	 * @param sourceFactory The source factory.
	 */
	public void setSourceFactory(SourceFactory sourceFactory) {
		Assert.notNull(sourceFactory, "SourceFactory must not be null");
		this.sourceFactory = sourceFactory;
	}

	/**
	 * Sets the ResultFactory.
	 * @param resultFactory The result factory.
	 */
	@Override
	public void setResultFactory(ResultFactory resultFactory) {
		super.setResultFactory(resultFactory);
		this.resultFactoryExplicitlySet = true;
	}

	/**
	 * Specify whether to always use source factory even for directly supported payload types.
	 * @param alwaysUseSourceFactory true to always use the source factory.
	 */
	public void setAlwaysUseSourceFactory(boolean alwaysUseSourceFactory) {
		this.alwaysUseSourceFactory = alwaysUseSourceFactory;
	}

	/**
	 * Specify whether to always use result factory even for directly supported payload types
	 * @param alwaysUseResultFactory true to always use the result factory.
	 */
	public void setAlwaysUseResultFactory(boolean alwaysUseResultFactory) {
		this.alwaysUseResultFactory = alwaysUseResultFactory;
	}

	public void setXslParameterMappings(Map<String, Expression> xslParameterMappings) {
		this.xslParameterMappings = xslParameterMappings;
	}

	public void setXsltParamHeaders(String... xsltParamHeaders) {
		Assert.notNull(xsltParamHeaders, "'xsltParamHeaders' must not be null.");
		this.xsltParamHeaders = Arrays.copyOf(xsltParamHeaders, xsltParamHeaders.length);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "'beanClassLoader' must not be null.");
		this.classLoader = classLoader;
	}

	@Override
	public void setResultType(String resultType) {
		super.setResultType(resultType);
		if (StringUtils.hasText(resultType)) {
			this.alwaysUseResultFactory = true;
		}
	}

	@Override
	public void setResultFactoryName(String resultFactoryName) {
		super.setResultFactoryName(resultFactoryName);
		if (StringUtils.hasText(resultFactoryName)) {
			this.alwaysUseResultFactory = true;
		}
	}

	@Override
	public String getComponentType() {
		return "xml:xslt-transformer";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (this.templates == null) {
			try {
				TransformerFactory transformerFactory = createTransformerFactory();
				this.templates = transformerFactory.newTemplates(createStreamSourceOnResource(this.xslResource));
			}
			catch (ClassNotFoundException | TransformerConfigurationException | IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private TransformerFactory createTransformerFactory() throws ClassNotFoundException {
		TransformerFactory transformerFactory;
		if (this.transformerFactoryClassName != null) {
			@SuppressWarnings("unchecked")
			Class<TransformerFactory> transformerFactoryClass =
					(Class<TransformerFactory>) ClassUtils.forName(this.transformerFactoryClassName, this.classLoader);
			transformerFactory = TransformerFactoryUtils.newInstance(transformerFactoryClass);
		}
		else {
			transformerFactory = TransformerFactoryUtils.newInstance();
		}
		try {
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file,jar:file");
		}
		catch (@SuppressWarnings("unused") IllegalArgumentException ex) {
			logger.info(() -> "The '" + XMLConstants.ACCESS_EXTERNAL_STYLESHEET + "' property is not supported by "
					+ transformerFactory.getClass().getCanonicalName());
		}
		return transformerFactory;
	}

	@Override
	protected Object doTransform(Message<?> message) {
		try {
			Transformer transformer = buildTransformer(message);
			Object payload;
			if (this.alwaysUseSourceFactory) {
				payload = this.sourceFactory.createSource(message.getPayload());
			}
			else {
				payload = message.getPayload();
			}
			Object transformedPayload;
			if (this.alwaysUseResultFactory) {
				transformedPayload = transformUsingResultFactory(payload, transformer);
			}
			else if (payload instanceof String string) {
				transformedPayload = transformString(string, transformer);
			}
			else if (payload instanceof Document document) {
				transformedPayload = transformDocument(document, transformer);
			}
			else if (payload instanceof Source source) {
				transformedPayload = transformSource(source, payload, transformer);
			}
			else {
				// fall back to trying factories
				transformedPayload = transformUsingResultFactory(payload, transformer);
			}
			return transformedPayload;
		}
		catch (TransformerException e) {
			throw new IllegalStateException(e);
		}
	}

	private Object transformUsingResultFactory(Object payload, Transformer transformer) throws TransformerException {
		Source source;
		if (this.alwaysUseSourceFactory) {
			source = this.sourceFactory.createSource(payload);
		}
		else if (payload instanceof String string) {
			source = new StringSource(string);
		}
		else if (payload instanceof Document document) {
			source = new DOMSource(document);
		}
		else if (payload instanceof Source castSource) {
			source = castSource;
		}
		else {
			source = this.sourceFactory.createSource(payload);
		}
		return transformSource(source, payload, transformer);
	}

	private Object transformSource(Source source, Object payload, Transformer transformer)
			throws TransformerException {

		Result result;
		if (!this.resultFactoryExplicitlySet &&
				"text".equals(transformer.getOutputProperties().getProperty("method"))) {
			result = new StringResult();
		}
		else {
			result = getResultFactory().createResult(payload);
		}
		transformer.transform(source, result);
		if (this.resultTransformer != null) {
			return this.resultTransformer.transformResult(result);
		}
		return result;
	}

	private String transformString(String stringPayload, Transformer transformer) throws TransformerException {
		StringResult result = new StringResult();
		Source source;
		if (this.alwaysUseSourceFactory) {
			source = this.sourceFactory.createSource(stringPayload);
		}
		else {
			source = new StringSource(stringPayload);
		}
		transformer.transform(source, result);
		return result.toString();
	}

	private Document transformDocument(Document documentPayload, Transformer transformer) throws TransformerException {
		Source source;
		if (this.alwaysUseSourceFactory) {
			source = this.sourceFactory.createSource(documentPayload);
		}
		else {
			source = new DOMSource(documentPayload);
		}
		Result result = getResultFactory().createResult(documentPayload);
		if (!DOMResult.class.isAssignableFrom(result.getClass())) {
			throw new MessagingException(
					"Document to Document conversion requires a DOMResult-producing ResultFactory implementation.");
		}
		DOMResult domResult = (DOMResult) result;
		transformer.transform(source, domResult);
		return (Document) domResult.getNode();
	}

	private Transformer buildTransformer(Message<?> message) throws TransformerException {
		// process individual mappings
		Transformer transformer = this.templates.newTransformer();
		if (this.xslParameterMappings != null) {
			for (Entry<String, Expression> entry : this.xslParameterMappings.entrySet()) {
				String parameterName = entry.getKey();
				Expression expression = entry.getValue();
				try {
					Object value = expression.getValue(this.evaluationContext, message);
					transformer.setParameter(parameterName, value);
				}
				catch (Exception e) {
					logger.warn(() -> "Evaluation of header expression '"
							+ expression.getExpressionString()
							+ "' failed. The XSLT parameter '"
							+ parameterName + "' will be skipped.");
				}
			}
		}
		// process xslt-parameter-headers
		if (!ObjectUtils.isEmpty(this.xsltParamHeaders)) {
			for (Entry<String, Object> entry : message.getHeaders().entrySet()) {
				String headerName = entry.getKey();
				if (PatternMatchUtils.simpleMatch(this.xsltParamHeaders, headerName)) {
					transformer.setParameter(headerName, entry.getValue());
				}
			}
		}
		return transformer;
	}

	/**
	 * Compensate for the fact that a Resource <i>may</i> not be a File or even
	 * addressable through a URI. If it is, we want the created StreamSource to
	 * read other resources relative to the provided one. If it isn't, it loads
	 * from the default path.
	 */
	private static StreamSource createStreamSourceOnResource(Resource xslResource) throws IOException {
		try {
			String systemId = xslResource.getURI().toString();
			return new StreamSource(xslResource.getInputStream(), systemId);
		}
		catch (IOException e) {
			return new StreamSource(xslResource.getInputStream());
		}
	}

}
