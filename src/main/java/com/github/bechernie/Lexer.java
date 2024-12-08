package com.github.bechernie;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Lexer {

    public record Lexeme(LexemeType type, int line, int columnStart, int columnEnd) {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Descriptor {
        String value();
    }

    public static String getDescriptorValue(LexemeType lexemeType) {
        return lexemeType.getClass().getAnnotation(Descriptor.class).value();
    }

    public sealed interface LexemeType {
    }

    @Descriptor("identifier")
    public record Identifier(String value) implements LexemeType {
    }

    @Descriptor("integer constant")
    public record IntConstant(int value) implements LexemeType {
    }

    @Descriptor("int")
    public record IntKeyword() implements LexemeType {
    }

    @Descriptor("void")
    public record VoidKeyword() implements LexemeType {
    }

    @Descriptor("return")
    public record ReturnKeyword() implements LexemeType {
    }

    @Descriptor("open parenthesis")
    public record OpenParenthesis() implements LexemeType {
    }

    @Descriptor("close parenthesis")
    public record CloseParenthesis() implements LexemeType {
    }

    @Descriptor("open brace")
    public record OpenBrace() implements LexemeType {
    }

    @Descriptor("close brace")
    public record CloseBrace() implements LexemeType {
    }

    @Descriptor("semicolon")
    public record Semicolon() implements LexemeType {
    }

    @Descriptor("minus")
    public record Minus() implements LexemeType {
    }

    @Descriptor("decrement")
    public record Decrement() implements LexemeType {
    }

    @Descriptor("bitwise complement")
    public record BitwiseComplement() implements LexemeType {
    }

    @Descriptor("eof")
    public record EOF() implements LexemeType {
    }

    public static class LexingException extends RuntimeException {
    }

    private static final Map<Pattern, Function<String, LexemeType>> LEXEMES_PATTERNS = new LinkedHashMap<>();

    static {
        LEXEMES_PATTERNS.put(Pattern.compile("\\w*\\b"), (matchedText) -> switch (matchedText) {
            case "int" -> new IntKeyword();
            case "void" -> new VoidKeyword();
            case "return" -> new Lexer.ReturnKeyword();
            default -> {
                final var intConstant = Pattern.compile("[0-9]+\\b");
                final var leadingChar = matchedText.getBytes()[0];

                if (intConstant.matcher(matchedText).matches()) {
                    yield new IntConstant(Integer.parseInt(matchedText));
                } else if (leadingChar > '0' && leadingChar < '9') {
                    throw new LexingException();
                }

                yield new Identifier(matchedText);
            }
        });
        LEXEMES_PATTERNS.put(Pattern.compile("--"), (_) -> new Decrement());
        LEXEMES_PATTERNS.put(Pattern.compile("-"), (_) -> new Minus());
        LEXEMES_PATTERNS.put(Pattern.compile("~"), (_) -> new BitwiseComplement());
        LEXEMES_PATTERNS.put(Pattern.compile("\\("), (_) -> new OpenParenthesis());
        LEXEMES_PATTERNS.put(Pattern.compile("\\)"), (_) -> new CloseParenthesis());
        LEXEMES_PATTERNS.put(Pattern.compile("\\{"), (_) -> new OpenBrace());
        LEXEMES_PATTERNS.put(Pattern.compile("}"), (_) -> new CloseBrace());
        LEXEMES_PATTERNS.put(Pattern.compile(";"), (_) -> new Semicolon());
    }

    public sealed interface LexResult {
    }

    public record Error(char currentChar, int line, int column) implements LexResult {
    }

    public record Success(List<Lexeme> lexemes) implements LexResult {
    }

    public LexResult lex(String program) {
        final var lexemes = new ArrayList<Lexeme>();

        var currentLine = 1;
        var currentColumn = 1;

        while (!program.isEmpty()) {
            if (program.getBytes()[0] == ' ') {
                program = program.substring(1);
                currentColumn++;
            } else if (program.getBytes()[0] == '\n') {
                program = program.substring(1);
                currentColumn = 1;
                currentLine++;
            } else {
                var longestMatch = "";
                LexemeType lexemeType = null;

                for (final var lexemePattern : LEXEMES_PATTERNS.entrySet()) {
                    final var lexemeMatcher = lexemePattern.getKey().matcher(program);

                    for (var i = program.length(); i > longestMatch.length(); i--) {
                        lexemeMatcher.reset();
                        lexemeMatcher.region(0, i);

                        if (lexemeMatcher.matches()) {
                            final var matchedGroup = lexemeMatcher.group();

                            if (matchedGroup.length() > longestMatch.length()) {
                                try {
                                    lexemeType = lexemePattern.getValue().apply(matchedGroup);
                                    longestMatch = matchedGroup;
                                } catch (LexingException e) {
                                    return new Error((char) program.getBytes()[0], currentLine, currentColumn);
                                }
                            }
                        }
                    }
                }

                if (lexemeType == null) {
                    return new Error((char) program.getBytes()[0], currentLine, currentColumn);
                }

                lexemes.add(new Lexeme(lexemeType, currentLine, currentColumn, currentColumn + longestMatch.length()));
                currentColumn += longestMatch.length();
                program = program.substring(longestMatch.length());
            }
        }

        lexemes.add(new Lexeme(new EOF(), currentLine, currentColumn, currentColumn));

        return new Success(lexemes);
    }
}
