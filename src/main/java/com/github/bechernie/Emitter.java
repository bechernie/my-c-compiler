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
                    printStream.println("pushq\t %rbp");
                    printStream.println("movq\t %rsp, %rbp");
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
                    "movl\t " + convertOperand(operand1) + ", " + convertOperand(operand2);
            case Codegen.Ret _ -> """
                    movq\t %rbp, %rsp
                    popq\t %rbp
                    ret
                    """;
            case Codegen.AllocateStack(int size) -> "subq\t $" + size + ", %rsp";
            case Codegen.Unary(Codegen.UnaryOperator operator, Codegen.Operand operand) ->
                    convertUnaryOperator(operator) + "\t " + convertOperand(operand);
        };
    }

    private String convertUnaryOperator(Codegen.UnaryOperator operator) {
        return switch (operator) {
            case Codegen.Neg _ -> "negl";
            case Codegen.Not _ -> "notl";
        };
    }

    private String convertOperand(Codegen.Operand operand) {
        return switch (operand) {
            case Codegen.Imm(int value) -> "$" + value;
            case Codegen.Register(Codegen.Reg reg) -> convertRegister(reg);
            case Codegen.Stack(int size) -> size + "(%rbp)";
            default -> throw new IllegalStateException("Unexpected value: " + operand);
        };
    }

    private String convertRegister(Codegen.Reg reg) {
        return switch (reg) {
            case Codegen.AX _ -> "%eax";
            case Codegen.R10 _ -> "%r10d";
        };
    }
}
