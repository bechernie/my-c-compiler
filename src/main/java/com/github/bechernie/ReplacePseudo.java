package com.github.bechernie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplacePseudo {

    private static final Map<String, Integer> STACK_OFFSETS = new HashMap<>();

    private int maxOffset = 0;

    public record Result(Codegen.Program program, int stackOffset) {
    }

    public Result emitAssembly(Codegen.Program program) {
        return switch (program) {
            case Codegen.Function(String name, List<Codegen.Instruction> instructions) ->
                    new Result(new Codegen.Function(name, convertInstructions(instructions)), maxOffset);
        };
    }

    private List<Codegen.Instruction> convertInstructions(List<Codegen.Instruction> instructions) {
        return instructions.stream().map(this::convertInstruction).toList();
    }

    private Codegen.Instruction convertInstruction(Codegen.Instruction instruction) {
        return switch (instruction) {
            case Codegen.AllocateStack allocateStack -> allocateStack;
            case Codegen.Mov(Codegen.Operand operand1, Codegen.Operand operand2) -> {
                if (operand1 instanceof Codegen.Pseudo(String identifier)) {
                    operand1 = convertPseudoRegister(identifier);
                }
                if (operand2 instanceof Codegen.Pseudo(String identifier)) {
                    operand2 = convertPseudoRegister(identifier);
                }
                yield new Codegen.Mov(operand1, operand2);
            }
            case Codegen.Ret ret -> ret;
            case Codegen.Unary(Codegen.UnaryOperator operator, Codegen.Operand operand) -> {
                if (operand instanceof Codegen.Pseudo(String identifier)) {
                    operand = convertPseudoRegister(identifier);
                }
                yield new Codegen.Unary(operator, operand);
            }
        };
    }

    private Codegen.Operand convertPseudoRegister(String identifier) {
        final var offset = STACK_OFFSETS.computeIfAbsent(identifier, _ -> {
            final var currentOffset = maxOffset;
            maxOffset += 4;
            return -currentOffset;
        });

        return new Codegen.Stack(offset);
    }
}
