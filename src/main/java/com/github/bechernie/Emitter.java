package com.github.bechernie;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class Emitter {

    public void emit(String outputPath, Codegen.Program assembly) {
        try (final var printStream = new PrintStream(new FileOutputStream(outputPath))) {
            switch (assembly) {
                case Codegen.Function(String name, List<Codegen.Instruction> instructions) -> {
                    printStream.println("\t.globl " + name);
                    printStream.println(name + ":");
                    instructions.forEach(instruction -> printStream.println("\t" + emitInstruction(instruction)));
                }
            }
            printStream.println(".section .note.GNU-stack,\"\",@progbits");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String emitInstruction(Codegen.Instruction instruction) {
        return switch (instruction) {
            case Codegen.Mov(Codegen.Operand operand1, Codegen.Operand operand2) ->
                    "movl " + convertOperand(operand1) + ", " + convertOperand(operand2);
            case Codegen.Ret _ -> "ret";
            case Codegen.AllocateStack allocateStack -> throw new UnsupportedOperationException();
            case Codegen.Unary unary -> throw new UnsupportedOperationException();
        };
    }

    private String convertOperand(Codegen.Operand operand) {
        return switch (operand) {
            case Codegen.Imm(int value) -> "$" + value;
            case Codegen.Register _ -> "%eax";
            case Codegen.Pseudo pseudo -> throw new UnsupportedOperationException();
            case Codegen.Stack stack -> throw new UnsupportedOperationException();
        };
    }
}
