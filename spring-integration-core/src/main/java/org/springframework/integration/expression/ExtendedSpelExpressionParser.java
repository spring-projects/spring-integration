package org.springframework.integration.expression;

import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * The {@link SpelExpressionParser} extension, which check the {@code expressionString}
 * to be single-quotes and return a {@link LiteralExpression} instance, otherwise delegate
 * to the super class.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class ExtendedSpelExpressionParser extends SpelExpressionParser {

	public ExtendedSpelExpressionParser() {
		super();
	}

	public ExtendedSpelExpressionParser(SpelParserConfiguration configuration) {
		super(configuration);
	}

	@Override
	public Expression parseExpression(String expressionString, ParserContext context) throws ParseException {
		if (expressionString.startsWith("'") && expressionString.endsWith("'")) {
			return new LiteralExpression(expressionString.replaceFirst("'", "").substring(0, expressionString.length() - 2));
		}
		else {
			return super.parseExpression(expressionString, context);
		}
	}

}
