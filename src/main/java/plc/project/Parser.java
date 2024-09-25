package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    //TODO: I need to change the way I use peek when I want to check for multiple options.
    //I believe I should be using Regex instead of a ',' becasue that chekcs for multiple things in a row.
    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        while (tokens.has(0)) {
            if (peek("LET")) {
                fields.add(parseField());
            } else if (peek("DEF")) {
                methods.add(parseMethod());
            } else {
                throw new ParseException("Expected LET or DEF", tokens.get(0).getIndex());
            }
        }

        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        match("LET");
        Optional<Ast.Expr> value = Optional.empty();
        if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (peek("=")) {
                match("=");
                Ast.Expr expression = parseExpression();
                value = Optional.of(expression);
            }
            if (peek(";")) {
                match(";");
                return new Ast.Field(name, value);
            } else {
                throw new ParseException("Expected ;", tokens.get(0).getIndex());
            }
        }
        else {
            throw new ParseException("Expected IDENTIFIER", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr recurse = parseEqualityExpression();
        String operator;
        //The above should match on everything I believe so we know need to handle 0+ comparisons and expressions.
        while (peek("\b(AND|OR)\b")) {
            if (peek("AND")) {
                operator = "AND";
                match("AND");
                recurse = new Ast.Expr.Binary(operator, recurse, parseEqualityExpression());
            } else {
                operator = "OR";
                match("OR");
                recurse = new Ast.Expr.Binary(operator, recurse, parseEqualityExpression());
            }

        }

        return recurse;

    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        //Base case
        Ast.Expr recurse = parseAdditiveExpression();
        //recurse of a kind
        while (peek("<=|>=|==|!=|<|>")) {
            if (peek("<")) {
                match("<");
                recurse = new Ast.Expr.Binary("<", recurse, parseAdditiveExpression());
            }
            else if (peek("<=")) {
                match("<=");
                recurse = new Ast.Expr.Binary("<=", recurse, parseAdditiveExpression());
            }
            else if (peek(">")) {
                match(">");
                recurse = new Ast.Expr.Binary(">", recurse, parseAdditiveExpression());
            }
            else if (peek(">=")) {
                match(">=");
                recurse = new Ast.Expr.Binary(">=", recurse, parseAdditiveExpression());
            }
            else if (peek("==")) {
                match("==");
                recurse = new Ast.Expr.Binary("==", recurse, parseAdditiveExpression());
            }
            else if (peek("!=")) {
                match("!=");
                recurse = new Ast.Expr.Binary("!=", recurse, parseAdditiveExpression());
            }
        }

        return recurse;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr recursive = parseMultiplicativeExpression();
        while (peek("+|-")) {
            if (peek("+")) {
                match("+");
                recursive = new Ast.Expr.Binary("+", recursive, parseMultiplicativeExpression());
            } else {
                match("-");
                recursive = new Ast.Expr.Binary("-", recursive, parseMultiplicativeExpression());
            }
        }
        return recursive;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr recursive = parseSecondaryExpression();
        while (peek("*|/")) {
            if (peek("*")) {
                match("*");
                recursive = new Ast.Expr.Binary("*", recursive, parseSecondaryExpression());
            } else {
                match("/");
                recursive = new Ast.Expr.Binary("/", recursive, parseSecondaryExpression());
            }
        }
        return recursive;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr recursive = parsePrimaryExpression();
        while (peek(".")) {
            match(".");
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected IDENTIFIER", tokens.get(0).getIndex());
            }
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            //TODO: I need to determine if it's an access or a call before doing this.

            //If it's a call.
            if (peek("(")) {
                List<Ast.Expr> expressions = new ArrayList<>();
                match("(");
                while (!peek(")")) {
                    expressions.add(parseExpression());
                    if (peek(",", ")")) {
                        throw new ParseException("Can't end call with ,", tokens.get(0).getIndex());
                    }
                    if (peek(",")) {
                        match(",");
                    }
                }
                match(")");
                recursive = new Ast.Expr.Function(Optional.of(recursive), name, expressions);
            }
            //It's an access.
            recursive = new Ast.Expr.Access(Optional.of(recursive), name);
        }
        return recursive;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        //If it's an expression in (
        if (peek("(")) {
            match("(");
            Ast.Expr toReturn = parseExpression();
            if (peek(")")) {
                match(")");
                return toReturn;
            } else {
                throw new ParseException("Expected )", tokens.get(0).getIndex());
            }
        }
        //If it's an identifier
        else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (!peek("(")) {
                return new Ast.Expr.Literal(name);
            }
            else {
                List<Ast.Expr> expressions = new ArrayList<>();
                match("(");
                while (!peek(")")) {
                    expressions.add(parseExpression());
                    if (peek(",", ")")) {
                        throw new ParseException("Can't end call with ,", tokens.get(0).getIndex());
                    }
                    if (peek(",")) {
                        match(",");
                    }
                }
                match(")");
                return new Ast.Expr.Function(Optional.of(new Ast.Expr.Literal(name)), name, expressions);
            }
        }
        else if(peek("\b(NIL|TRUE|FALSE)\b")) {
            if (peek("NIL")) {
                match("NIL");
                return new Ast.Expr.Literal(null);
            }
            else if (peek("TRUE")) {
                match("TRUE");
                return new Ast.Expr.Literal(true);
            }
            else {
                match("FALSE");
                return new Ast.Expr.Literal(false);
            }
        }
        else if (peek(Token.Type.IDENTIFIER)) {
            String literal = tokens.get(0).getLiteral();
             return new Ast.Expr.Literal(literal);
        }
        else if (peek(Token.Type.INTEGER)) {

        }

    }


    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
             else if (patterns[i] instanceof Token.Type) {
                 if (patterns[i] != tokens.get(i).getType()) {
                     return false;
                 }
            }
                else if (patterns[i] instanceof String) {
                    if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                        return false;
                    }
                }
                else {
                    throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++ ) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
