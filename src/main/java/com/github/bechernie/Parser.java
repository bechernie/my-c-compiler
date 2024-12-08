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

    public record Unary(UnaryOperator operator, Expression expression) implements Expression {
    }

    public sealed interface UnaryOperator {
    }

    public record Negate() implements UnaryOperator {
    }

    public record BitwiseComplement() implements UnaryOperator {
    }

    public sealed interface ParseResult {
    }

    public record Error(List<Lexer.LexemeType> expected, Lexer.Lexeme actual) implements ParseResult {
    }

    public record Success(Program program) implements ParseResult {
    }

    private static class ParseException extends RuntimeException {

        private final List<Lexer.LexemeType> expected;
        private final Lexer.Lexeme actual;

        public ParseException(Lexer.LexemeType expected, Lexer.Lexeme actual) {
            this.expected = List.of(expected);
            this.actual = actual;
        }

        public ParseException(List<Lexer.LexemeType> expected, Lexer.Lexeme actual) {
            this.expected = expected;
            this.actual = actual;
        }

        public List<Lexer.LexemeType> getExpected() {
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
        final var nextToken = peek(lexemes);
        return switch (nextToken.type()) {
            case Lexer.IntConstant _, Lexer.IntKeyword() -> parseInt(lexemes);
            case Lexer.Minus(), Lexer.BitwiseComplement() -> {
                final var operator = parseUnaryOperator(lexemes);
                final var innerExpression = parseExpression(operator.rest);
                yield new Result<>(new Unary(operator.item, innerExpression.item), innerExpression.rest);
            }
            case Lexer.OpenParenthesis() -> {
                final var openParenthesisRest = expect(new Lexer.OpenParenthesis(), lexemes);
                final var innerExpression = parseExpression(openParenthesisRest);
                final var closeParenthesisRest = expect(new Lexer.CloseParenthesis(), innerExpression.rest);
                yield new Result<>(innerExpression.item, closeParenthesisRest);
            }
            default ->
                    throw new ParseException(List.of(new Lexer.IntKeyword(), new Lexer.Minus(), new Lexer.OpenParenthesis()), nextToken);
        };
    }

    private Result<UnaryOperator> parseUnaryOperator(List<Lexer.Lexeme> lexemes) {
        final var token = lexemes.getFirst();
        return switch (token.type()) {
            case Lexer.Minus() -> new Result<>(new Negate(), lexemes.subList(1, lexemes.size()));
            case Lexer.BitwiseComplement() -> new Result<>(new BitwiseComplement(), lexemes.subList(1, lexemes.size()));
            default -> throw new ParseException(List.of(new Lexer.Minus(), new Lexer.BitwiseComplement()), token);
        };
    }

    private Result<Expression> parseInt(List<Lexer.Lexeme> lexemes) {
        final var actual = lexemes.getFirst();
        return switch (actual.type()) {
            case Lexer.IntConstant(int value) -> new Result<>(new Constant(value), lexemes.subList(1, lexemes.size()));
            default -> throw new ParseException(new Lexer.IntConstant(0), actual);
        };
    }

    private Lexer.Lexeme peek(List<Lexer.Lexeme> lexemes) {
        return lexemes.getFirst();
    }

    private List<Lexer.Lexeme> expect(Lexer.LexemeType expected, List<Lexer.Lexeme> lexemes) {
        final var actual = lexemes.getFirst();
        if (!actual.type().equals(expected)) {
            throw new ParseException(expected, actual);
        }
        return lexemes.subList(1, lexemes.size());
    }
}
