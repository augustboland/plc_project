package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
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
            System.out.println(peek(";"));
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
        match("DEF");

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected IDENTIFIER", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        List<String> parameters = new ArrayList<>();
        if (!peek("(")) {
            throw new ParseException("Expected (", tokens.get(0).getIndex());
        }
        match("(");
        while(!peek(")")) {
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected IDENTIFIER", tokens.get(0).getIndex());
            }
            parameters.add(tokens.get(0).getLiteral());
            match(Token.Type.IDENTIFIER);
            if (peek(",", ")")) {
                throw new ParseException("Can't end with ,", tokens.get(0).getIndex());
            }
            if (peek(",")) {
                match(",");
            }
        }
        match(")");

        if (!peek("DO")) {
            throw new ParseException("Expected DO", tokens.get(0).getIndex());
        }

        List<Ast.Stmt> statements = new ArrayList<>();
        match("DO");
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        match("END");

        return new Ast.Method(name, parameters, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("FOR")) {
            return parseForStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else if (peek(Token.Type.IDENTIFIER)) {
            Ast.Expr expr = parseExpression();

            if (peek("=")) {
                match("=");

                Ast.Expr value = parseExpression();
                if(peek(";")){
                    match(";");
                    return new Ast.Stmt.Assignment(expr, value);
                }else{
                    throw new ParseException("Needed a ; at the end", tokens.get(0).getIndex());
                }
//                return new Ast.Stmt.Assignment(expr, value);
            }

            if(peek(";")){
                match(";");
                return new Ast.Stmt.Expression(expr);
            }else{
                System.out.println("hehehe");
                throw new ParseException("Needed a ; at the end", 0);
            }
        }
        else {
//            System.out.println("HERE");
//            System.out.println(tokens.get(-1).getLiteral());
            System.out.println(tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex());
            throw new ParseException("Expected LET, IF, FOR, WHILE, RETURN, or IDENTIFIER", tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex());
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected IDENTIFIER", tokens.get(0).getIndex());
        }

        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

//        if (!peek("=")) {
//            throw new ParseException("Expected =", tokens.get(0).getIndex());
//        }

        Optional<Ast.Expr> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        System.out.println(tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        System.out.println(tokens.get(0).getIndex());
        if(match(";")){
            return new Ast.Stmt.Declaration(name, value);
        }else{
            throw new ParseException("Expected ;", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expr condition = parseExpression();
        if (!peek("DO")) {
            throw new ParseException("Expected DO", tokens.get(0).getIndex());
        }
        match("DO");
        List<Ast.Stmt> statements = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            statements.add(parseStatement());
        }

        if (match("ELSE")) {
//            match("ELSE");
            List<Ast.Stmt> elseStatements = new ArrayList<>();
            while (!peek("END")) {
                elseStatements.add(parseStatement());
            }
            match("END");
            return new Ast.Stmt.If(condition, statements, elseStatements);
        }

        match("END");
        return new Ast.Stmt.If(condition, statements, new ArrayList<>());
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        match("FOR");

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected IDENTIFIER", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        if (!peek("IN")) {
            throw new ParseException("Expected IN", tokens.get(0).getIndex());
        }
        match("IN");

        Ast.Expr value = parseExpression();

        if (!peek("DO")) {
            throw new ParseException("Expected DO", tokens.get(0).getIndex());
        }
        match("DO");

        List<Ast.Stmt> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        match("END");
        return new Ast.Stmt.For(name, value, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expr condition = parseExpression();
        if (!peek("DO")) {
            throw new ParseException("Expected DO", tokens.get(0).getIndex());
        }
        System.out.println("here");
        match("DO");
        System.out.println("here");
        List<Ast.Stmt> statements = new ArrayList<>();
        while (!peek("END")) { // ||
            System.out.println(peek("."));
            statements.add(parseStatement());
            System.out.println(peek("END"));
//            if(token)
        }

        System.out.println("here");
        if (!peek("END")) {
            System.out.println("here");
            throw new ParseException("Expected END", 1);
        }
        match("END");
        return new Ast.Stmt.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expr value = parseExpression();
        match(";");
        return new Ast.Stmt.Return(value);
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
        while (true) {
            if (match("AND")) {
                recurse = new Ast.Expr.Binary("AND", recurse, parseEqualityExpression());
            } else if (match("OR")) {
                recurse = new Ast.Expr.Binary("OR", recurse, parseEqualityExpression());
            } else {
                break;
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
        while (true) {
            if (match("<")) {
                recurse = new Ast.Expr.Binary("<", recurse, parseAdditiveExpression());
            } else if (match("<=")) {
                recurse = new Ast.Expr.Binary("<=", recurse, parseAdditiveExpression());
            } else if (match(">")) {
                recurse = new Ast.Expr.Binary(">", recurse, parseAdditiveExpression());
            } else if (match(">=")) {
                recurse = new Ast.Expr.Binary(">=", recurse, parseAdditiveExpression());
            } else if (match("==")) {
                recurse = new Ast.Expr.Binary("==", recurse, parseAdditiveExpression());
            } else if (match("!=")) {
                recurse = new Ast.Expr.Binary("!=", recurse, parseAdditiveExpression());
            } else {
                break;
            }
        }

        return recurse;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr recursive = parseMultiplicativeExpression();
        while (true) {
            if (match("+")) {
                Ast.Expr right = parseMultiplicativeExpression();
                recursive = new Ast.Expr.Binary("+", recursive, right);
            } else if (match("-")) {
                Ast.Expr right = parseMultiplicativeExpression();
                recursive = new Ast.Expr.Binary("-", recursive, right);
            } else {
                break;
            }
        }
        return recursive;
    }
    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr recursive = parseSecondaryExpression();
        while (true) {
            if (match("*")) {
                Ast.Expr right = parseSecondaryExpression();
                recursive = new Ast.Expr.Binary("*", recursive, right);
            } else if (match("/")) {
                Ast.Expr right = parseSecondaryExpression();
                recursive = new Ast.Expr.Binary("/", recursive, right);
            } else {
                break;
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
            else {
                //It's an access.
                recursive = new Ast.Expr.Access(Optional.of(recursive), name);
            }
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
        if (match("(")) {
//            match("(");
            Ast.Expr toReturn = parseExpression();
            if (peek(")")) {
                match(")");
                return new Ast.Expr.Group(toReturn);
            } else {
                throw new ParseException("Expected )", tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex());
            }
        }
        else if (peek("NIL")) {
                match("NIL");
                return new Ast.Expr.Literal(null);
            }
        else if (peek("TRUE")) {
                match("TRUE");
                return new Ast.Expr.Literal(true);
            }
        else if (peek("FALSE")) {
                match("FALSE");
                return new Ast.Expr.Literal(false);
            }
        else if (peek(Token.Type.IDENTIFIER)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
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
                return new Ast.Expr.Function(Optional.empty(), literal, expressions);
            }
            else {
                return new Ast.Expr.Access(Optional.empty(), literal);
            }
        }
        else if (peek(Token.Type.INTEGER)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.INTEGER);
            return new Ast.Expr.Literal(new BigInteger(literal));
        }
        else if (peek(Token.Type.DECIMAL)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.DECIMAL);
            return new Ast.Expr.Literal(new BigDecimal(literal));
        }
        else if (peek(Token.Type.CHARACTER)) {
            char literal = tokens.get(0).getLiteral().charAt(1);
            match(Token.Type.CHARACTER);
            return new Ast.Expr.Literal(literal);
        }
        else if (peek(Token.Type.STRING)) {
            String literal = tokens.get(0).getLiteral();
            literal = literal.substring(1, literal.length() - 1);
            literal = unescapeString(literal);
            match(Token.Type.STRING);
            return new Ast.Expr.Literal(literal);
        }
        //If it's an identifier
        else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (!peek("(")) {
                return new Ast.Expr.Access(Optional.empty(), name);
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
        else {
//            System.out.println(tokens.get(-1).getLiteral());
//            System.out.println(tokens.get(0).getLiteral());
//            System.out.println(tokens.get(0).getLiteral());
            throw new ParseException("Expected IDENTIFIER, INTEGER, DECIMAL, CHARACTER, STRING, NIL, TRUE, or FALSE", tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex());
        }

    }
    //This is taken from StackOverflow
    private String unescapeString(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case 'n': result.append('\n'); break; // Newline
                    case 't': result.append('\t'); break; // Tab
                    case 'r': result.append('\r'); break; // Carriage return
                    case 'b': result.append('\b'); break; // Backspace
                    case '\\': result.append('\\'); break; // Backslash
                    case '"': result.append('"'); break; // Double quote
                    case '\'': result.append('\''); break; // Single quote
                    default: result.append('\\').append(next); break; // Invalid escape
                }
                i++; // Skip the next character as it's already processed
            } else {
                result.append(current); // Regular character
            }
        }
        return result.toString();
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
