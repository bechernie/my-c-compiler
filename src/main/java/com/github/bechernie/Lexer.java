package com.github.bechernie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Lexer {

    record Lexeme(LexemeType type, int line, int columnStart, int columnEnd) {
    }

    public sealed interface LexemeType {
    }

    record Identifier(String value) implements LexemeType {
    }

    record Constant(int value) implements LexemeType {
    }

    record IntKeyword() implements LexemeType {
    }

    record VoidKeyword() implements LexemeType {
    }

    record ReturnKeyword() implements LexemeType {
    }

    record OpenParenthesis() implements LexemeType {
    }

    record CloseParenthesis() implements LexemeType {
    }

    record OpenBrace() implements LexemeType {
    }

    record CloseBrace() implements LexemeType {
    }

    record Semicolon() implements LexemeType {
    }

    static class LexingException extends RuntimeException {
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
                    yield new Constant(Integer.parseInt(matchedText));
                } else if (leadingChar > '0' && leadingChar < '9') {
                    throw new LexingException();
                }

                yield new Identifier(matchedText);
            }
        });
        LEXEMES_PATTERNS.put(Pattern.compile("\\("), (_) -> new OpenParenthesis());
        LEXEMES_PATTERNS.put(Pattern.compile("\\)"), (_) -> new CloseParenthesis());
        LEXEMES_PATTERNS.put(Pattern.compile("\\{"), (_) -> new OpenBrace());
        LEXEMES_PATTERNS.put(Pattern.compile("}"), (_) -> new CloseBrace());
        LEXEMES_PATTERNS.put(Pattern.compile(";"), (_) -> new Semicolon());
    }

    public sealed interface LexResult {
    }

    record Error(char currentChar, int line, int column) implements LexResult {
    }

    record Success(List<Lexeme> lexemes) implements LexResult {
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

        return new Success(lexemes);
    }
}
