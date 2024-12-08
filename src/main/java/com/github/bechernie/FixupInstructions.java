package com.github.bechernie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FixupInstructions {
    public Codegen.Program emitAssembly(Codegen.Program program, int stackOffset) {
        return switch (program) {
            case Codegen.Function(String name, List<Codegen.Instruction> instructions) -> {
                final var newInstructions = new ArrayList<>(rewriteInvalidMovs(instructions));
                newInstructions.addFirst(new Codegen.AllocateStack(stackOffset));
                yield new Codegen.Function(name, newInstructions);
            }
        };
    }

    private List<Codegen.Instruction> rewriteInvalidMovs(List<Codegen.Instruction> instructions) {
        return instructions.stream().map(this::rewriteInvalidMov).flatMap(Collection::stream).toList();
    }

    private List<Codegen.Instruction> rewriteInvalidMov(Codegen.Instruction instruction) {
        return switch (instruction) {
            case Codegen.AllocateStack allocateStack -> List.of(allocateStack);
            case Codegen.Mov(Codegen.Operand operand1, Codegen.Operand operand2) -> {
                if (operand1 instanceof Codegen.Stack(int size) && operand2 instanceof Codegen.Stack _) {
                    yield List.of(
                            new Codegen.Mov(new Codegen.Stack(size), new Codegen.Register(new Codegen.R10())),
                            new Codegen.Mov(new Codegen.Register(new Codegen.R10()), operand2)
                    );
                }
                yield List.of(new Codegen.Mov(operand1, operand2));
            }
            case Codegen.Ret ret -> List.of(ret);
            case Codegen.Unary unary -> List.of(unary);
        };
    }
}
