/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.language.simple;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tokenizer to create {@link SimpleToken} from the input.
 */
public final class SimpleTokenizer {

    // use CopyOnWriteArrayList so we can modify it in the for loop when changing function start/end tokens
    private static final List<SimpleTokenType> KNOWN_TOKENS = new CopyOnWriteArrayList<SimpleTokenType>();

    static {
        // add known tokens
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.whiteSpace, " "));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.singleQuote, "'"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.doubleQuote, "\""));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.functionStart, "${"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.functionStart, "$simple{"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.functionEnd, "}"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.booleanValue, "true"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.booleanValue, "false"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.nullValue, "null"));

        // binary operators
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "=="));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, ">="));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "<="));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, ">"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "<"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "!="));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "not is"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "is"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "not contains"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "contains"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "not regex"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "regex"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "not in"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "in"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "range"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.binaryOperator, "not range"));

        // unary operators
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.unaryOperator, "++"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.unaryOperator, "--"));

        // logical operators
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.logicalOperator, "&&"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.logicalOperator, "||"));
        // TODO: @deprecated logical operators, to be removed in Camel 3.0
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.logicalOperator, "and"));
        KNOWN_TOKENS.add(new SimpleTokenType(TokenType.logicalOperator, "or"));
    }

    private SimpleTokenizer() {
        // static methods
    }


    /**
     * @see SimpleLanguage#changeFunctionStartToken(String...)
     */
    public static void changeFunctionStartToken(String... startToken) {
        for (SimpleTokenType type : KNOWN_TOKENS) {
            if (type.getType() == TokenType.functionStart) {
                KNOWN_TOKENS.remove(type);
            }
        }

        // add in start of list as its a more common token to be used
        for (String token : startToken) {
            KNOWN_TOKENS.add(0, new SimpleTokenType(TokenType.functionStart, token));
        }
    }

    /**
     * @see SimpleLanguage#changeFunctionEndToken(String...)
     */
    public static void changeFunctionEndToken(String... endToken) {
        for (SimpleTokenType type : KNOWN_TOKENS) {
            if (type.getType() == TokenType.functionEnd) {
                KNOWN_TOKENS.remove(type);
            }
        }

        // add in start of list as its a more common token to be used
        for (String token : endToken) {
            KNOWN_TOKENS.add(0, new SimpleTokenType(TokenType.functionEnd, token));
        }
    }

    /**
     * Create the next token
     *
     * @param expression  the input expression
     * @param index       the current index
     * @param filter      defines the accepted token types to be returned (character is always used as fallback)
     * @return the created token, will always return a token
     */
    public static SimpleToken nextToken(String expression, int index, TokenType... filter) {
        return doNextToken(expression, index, filter);
    }

    /**
     * Create the next token
     *
     * @param expression  the input expression
     * @param index       the current index
     * @return the created token, will always return a token
     */
    public static SimpleToken nextToken(String expression, int index) {
        return doNextToken(expression, index);
    }

    private static SimpleToken doNextToken(String expression, int index, TokenType... filters) {

        boolean numericAllowed = acceptType(TokenType.numericValue, filters);
        if (numericAllowed) {
            // is it a numeric value
            StringBuilder sb = new StringBuilder();
            boolean digit = true;
            while (digit && index < expression.length()) {
                digit = Character.isDigit(expression.charAt(index));
                if (digit) {
                    char ch = expression.charAt(index);
                    sb.append(ch);
                    index++;
                }
            }
            if (sb.length() > 0) {
                return new SimpleToken(new SimpleTokenType(TokenType.numericValue, sb.toString()), index);
            }
        }

        // it could be any of the known tokens
        String text = expression.substring(index);
        for (SimpleTokenType token : KNOWN_TOKENS) {
            if (acceptType(token.getType(), filters)) {
                if (text.startsWith(token.getValue())) {
                    return new SimpleToken(token, index);
                }
            }
        }

        // fallback and create a character token
        char ch = expression.charAt(index);
        SimpleToken token = new SimpleToken(new SimpleTokenType(TokenType.character, "" + ch), index);
        return token;
    }

    private static boolean acceptType(TokenType type, TokenType... filters) {
        if (filters == null || filters.length == 0) {
            return true;
        }
        for (TokenType filter : filters) {
            if (type == filter) {
                return true;
            }
        }
        return false;
    }

}