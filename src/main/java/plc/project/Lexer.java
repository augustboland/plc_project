package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {

            while (peek("\\s")) {
                match("\\s");
                chars.skip();
            }
            tokens.add(lexToken());
        }
        return tokens;

    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     * <p>
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     *
     * @return
     */
    public Token lexToken() {

        if (!chars.has(0)){
            throw new ParseException("Unexpected end of Input", chars.index);
        }
        if (peek(" ")){
            throw new ParseException("Unexpected space", chars.index);
        } else if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        }
        else if (peek("[+-]?", "[0-9]")) {
            return lexNumber();
        }
        else if (peek("[0-9]")) {
            return lexNumber();
        }
        else if (peek("'")) {
            return lexCharacter();
        }
        else if (peek("\"")) {
            return lexString();
        }
        //TODO: test this as I believe we already catch white space, but this could be faulty.
        else {
            return lexOperator();
        }
    }


    public Token lexIdentifier() {
        match("[A-Za-z_]");
        while (peek("[A-Za-z0-9_-]")) {
            match("[A-Za-z0-9_-]");
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        if (peek ("[+-]")) {
            match("[+-]");
        }
        while (peek("[0-9]")) {
            match("[0-9]");
        }
        if (peek("\\.", "[0-9]")) {
            match("\\.");
            while (peek("[0-9]")) {
                match("[0-9]");
            }
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);
    }


    //TODO: I am unsure how we should be handling /r and /n(its given as a valid example but stated to not be valid.)
    public Token lexCharacter() {
        match("'");
        if (peek("\\\\")) {
            match("\\\\");
            if (peek("[^bnrt'\"\\\\]")) {
                throw new ParseException("Invalid Escape", chars.index);
            }
            else {
                lexEscape();
                match("[bnrt'\"\\\\]");
            }
        }
        else if (peek("'")) {
            throw new ParseException("Empty Character", chars.index);
        }
        else {
            match(".");
        }
        if (peek("'")) {
            match("'");
            return chars.emit(Token.Type.CHARACTER);
        }
        else {
            throw new ParseException("Improper Character", chars.index);
        }

    }

    public Token lexString() {
        match("\"");
        while (!peek("\"")) {
            if (!chars.has(0)) {
                throw new ParseException("Unterminated String", chars.index);
            }
            else
            if (peek("\\\\")) {
                match("\\\\");
                if (peek("[^bnrt'\"\\\\]")) {
                    throw new ParseException("Invalid Escape", chars.index);
                }
                else {
                    lexEscape();
                    match("[bnrt'\"\\\\]");
                }
            }
            else if (peek("[^\"\\\\]")) {
                match("[^\"\\\\]");
            }
            else {
                throw new ParseException("Invalid String", chars.index);
            }
        }
        match("\"");
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        //TODO: unsure of proper usage for this method.
    }

    public Token lexOperator() {
        if (peek("[!=><]", "=")) {
            match("[!=><]", "=");
            return chars.emit(Token.Type.OPERATOR);
        } else {
            match(".");
            return chars.emit(Token.Type.OPERATOR);
        }
    }

    public void skip(){
        if(chars.has(0)){
            if(chars.get(0) == ' '){
                match(" ");
            }
        }
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for ( int i = 0; i < patterns.length; i++ ) {
            if ( !chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i]) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++ ) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            //System.out.println( input.substring(start, index) + " | " + start + " | " + length + " | " + index);
            skip();
            //System.out.println( input.substring(start, index) + " | " + start + " | " + length + " | " + index);
            return new Token(type, input.substring(start, index), start);
        }

    }

}
