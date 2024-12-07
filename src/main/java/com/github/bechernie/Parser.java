package com.github.bechernie;

import java.util.List;

public class Parser {

    public sealed interface Program {
    }

    public record FunctionDefinition(String name, Statement body) implements Program {
    }

    public sealed interface Statement {
    }

    public record Return(Expression expression) implements Statement {
    }

    public sealed interface Expression {
    }

    public record Constant(int value) implements Expression {
    }

    public sealed interface ParseResult {
    }

    public record Error(Lexer.LexemeType expected, Lexer.Lexeme actual) implements ParseResult {
    }

    public record Success(Program program) implements ParseResult {
    }

    private static class ParseException extends RuntimeException {

        private final Lexer.LexemeType expected;
        private final Lexer.Lexeme actual;

        public ParseException(Lexer.LexemeType expected, Lexer.Lexeme actual) {
            this.expected = expected;
            this.actual = actual;
        }

        public Lexer.LexemeType getExpected() {
            return expected;
        }

        public Lexer.Lexeme getActual() {
            return actual;
        }
    }

    public ParseResult parseProgram(List<Lexer.Lexeme> lexemes) {
        try {
            final var functionDefinition = parseFunction(lexemes);
            expect(new Lexer.EOF(), functionDefinition.rest);
            return new Success(functionDefinition.item);
        } catch (ParseException e) {
            return new Error(e.getExpected(), e.getActual());
        }
    }

    private record Result<T>(T item, List<Lexer.Lexeme> rest) {
    }

    private Result<FunctionDefinition> parseFunction(List<Lexer.Lexeme> lexemes) {
        var rest = expect(new Lexer.IntKeyword(), lexemes);
        final var identifier = parseIdentifier(rest);
        rest = expect(new Lexer.OpenParenthesis(), identifier.rest);
        rest = expect(new Lexer.VoidKeyword(), rest);
        rest = expect(new Lexer.CloseParenthesis(), rest);
        rest = expect(new Lexer.OpenBrace(), rest);
        final var statement = parseStatement(rest);
        rest = expect(new Lexer.CloseBrace(), statement.rest);
        return new Result<>(new FunctionDefinition(identifier.item, statement.item), rest);
    }

    private Result<String> parseIdentifier(List<Lexer.Lexeme> lexemes) {
        final var actual = lexemes.getFirst();
        return switch (actual.type()) {
            case Lexer.Identifier(String value) -> new Result<>(value, lexemes.subList(1, lexemes.size()));
            default -> throw new ParseException(new Lexer.Identifier(""), actual);
        };
    }

    private Result<Statement> parseStatement(List<Lexer.Lexeme> lexemes) {
        var rest = expect(new Lexer.ReturnKeyword(), lexemes);
        final var expression = parseExpression(rest);
        rest = expect(new Lexer.Semicolon(), expression.rest);
        return new Result<>(new Return(expression.item), rest);
    }

    private Result<Expression> parseExpression(List<Lexer.Lexeme> lexemes) {
        return parseInt(lexemes);
    }

    private Result<Expression> parseInt(List<Lexer.Lexeme> lexemes) {
        final var actual = lexemes.getFirst();
        return switch (actual.type()) {
            case Lexer.IntConstant(int value) -> new Result<>(new Constant(value), lexemes.subList(1, lexemes.size()));
            default -> throw new ParseException(new Lexer.IntConstant(0), actual);
        };
    }

    private List<Lexer.Lexeme> expect(Lexer.LexemeType expected, List<Lexer.Lexeme> lexemes) {
        final var actual = lexemes.getFirst();
        if (!actual.type().equals(expected)) {
            throw new ParseException(expected, actual);
        }
        return lexemes.subList(1, lexemes.size());
    }
}
