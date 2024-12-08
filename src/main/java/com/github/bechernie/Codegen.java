package com.github.bechernie;

import java.util.List;

public class Codegen {

    public sealed interface Program {
    }

    public record Function(String name, List<Instruction> instructions) implements Program {
    }

    public sealed interface Instruction {
    }

    public record Mov(Operand operand1, Operand operand2) implements Instruction {
    }

    public record Ret() implements Instruction {
    }

    public sealed interface Operand {
    }

    public record Imm(int value) implements Operand {
    }

    public record Register() implements Operand {
    }

    public Program emitAssembly(Parser.Program program) {
        switch (program) {
            case Parser.FunctionDefinition(String name, Parser.Statement body) -> {
                return new Function(name, convertStatement(body));
            }
        }
    }

    private List<Instruction> convertStatement(Parser.Statement statement) {
        return switch (statement) {
            case Parser.Return(Parser.Expression expression) ->
                    List.of(new Mov(convertExpression(expression), new Register()), new Ret());
        };
    }

    private Operand convertExpression(Parser.Expression expression) {
        return switch (expression) {
            case Parser.Constant(int value) -> new Imm(value);
            case Parser.Unary(Parser.UnaryOperator operator, Parser.Expression innerExpression) ->
                    throw new UnsupportedOperationException();
        };
    }
}
