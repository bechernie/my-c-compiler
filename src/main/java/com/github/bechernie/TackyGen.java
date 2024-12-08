package com.github.bechernie;

import java.util.ArrayList;
import java.util.List;

public class TackyGen {

    private int counter = 0;

    public sealed interface Program {
    }

    public record Function(String name, List<Instruction> body) implements Program {
    }

    public sealed interface Instruction {
    }

    public record Return(Val val) implements Instruction {
    }

    public record Unary(UnaryOperator operator, Val source, Val destination) implements Instruction {
    }

    public sealed interface Val {
    }

    public record Constant(int value) implements Val {
    }

    public record Var(String identifier) implements Val {
    }

    public sealed interface UnaryOperator {
    }

    public record Negate() implements UnaryOperator {
    }

    public record BitwiseComplement() implements UnaryOperator {
    }

    public Program emitTacky(Parser.Program program) {
        return switch (program) {
            case Parser.FunctionDefinition(String name, Parser.Statement body) -> new Function(name, emitTacky(body));
        };
    }

    private List<Instruction> emitTacky(Parser.Statement statement) {
        return switch (statement) {
            case Parser.Return(Parser.Expression expression) -> {
                final var instructions = new ArrayList<Instruction>();
                final var returnInstruction = new Return(emitTacky(expression, instructions));
                instructions.add(returnInstruction);
                yield instructions;
            }
        };
    }

    private Val emitTacky(Parser.Expression expression, List<Instruction> instructions) {
        return switch (expression) {
            case Parser.Constant(int value) -> new Constant(value);
            case Parser.Unary(Parser.UnaryOperator operator, Parser.Expression innerExpression) -> {
                final var source = emitTacky(innerExpression, instructions);
                final var destinationName = makeTemporary();
                final var destination = new Var(destinationName);
                final var tackyOperator = convertUnaryOperator(operator);
                instructions.add(new Unary(tackyOperator, source, destination));
                yield destination;
            }
        };
    }

    private UnaryOperator convertUnaryOperator(Parser.UnaryOperator operator) {
        return switch (operator) {
            case Parser.BitwiseComplement _ -> new BitwiseComplement();
            case Parser.Negate _ -> new Negate();
        };
    }

    private String makeTemporary() {
        counter++;
        return "tmp." + counter;
    }
}
