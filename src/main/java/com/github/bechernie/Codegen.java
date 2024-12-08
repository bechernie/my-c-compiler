package com.github.bechernie;

import java.util.Collection;
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

    public record Unary(UnaryOperator operator, Operand operand) implements Instruction {
    }

    public record AllocateStack(int size) implements Instruction {
    }

    public record Ret() implements Instruction {
    }

    public sealed interface UnaryOperator {
    }

    public record Neg() implements UnaryOperator {
    }

    public record Not() implements UnaryOperator {
    }

    public sealed interface Operand {
    }

    public record Imm(int value) implements Operand {
    }

    public record Register(Reg reg) implements Operand {
    }

    public record Pseudo(String identifier) implements Operand {
    }

    public record Stack(int size) implements Operand {
    }

    public sealed interface Reg {
    }

    public record AX() implements Reg {
    }

    public record R10() implements Reg {
    }

    public Program emitAssembly(TackyGen.Program program) {
        return switch (program) {
            case TackyGen.Function(String name, List<TackyGen.Instruction> body) ->
                    new Function(name, emitAssembly(body));
        };
    }

    private List<Instruction> emitAssembly(List<TackyGen.Instruction> instructions) {
        return instructions.stream().map(this::emitAssembly).flatMap(Collection::stream).toList();
    }

    private List<Instruction> emitAssembly(TackyGen.Instruction instruction) {
        return switch (instruction) {
            case TackyGen.Return(TackyGen.Val val) -> List.of(
                    new Mov(convertVal(val), new Register(new AX())),
                    new Ret()
            );
            case TackyGen.Unary(TackyGen.UnaryOperator operator, TackyGen.Val source, TackyGen.Val destination) ->
                    List.of(
                            new Mov(convertVal(source), convertVal(destination)),
                            new Unary(convertUnaryOperator(operator), convertVal(destination))
                    );
        };
    }

    private Operand convertVal(TackyGen.Val val) {
        return switch (val) {
            case TackyGen.Constant(int value) -> new Imm(value);
            case TackyGen.Var(String identifier) -> new Pseudo(identifier);
        };
    }

    private UnaryOperator convertUnaryOperator(TackyGen.UnaryOperator operator) {
        return switch (operator) {
            case TackyGen.BitwiseComplement _ -> new Not();
            case TackyGen.Negate _ -> new Neg();
        };
    }
}
