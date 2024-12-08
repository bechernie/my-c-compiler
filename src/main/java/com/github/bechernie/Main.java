package com.github.bechernie;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class Main {

    record CompileOptions(boolean lex, boolean parse, boolean tacky, boolean codegen) {
    }

    public static void main(String[] args) {
        final var parser = new DefaultParser();

        final var options = new Options();
        options.addOption("l", "lex", false, "Stops before parsing");
        options.addOption("p", "parse", false, "Stops before tacky generation");
        options.addOption("t", "tacky", false, "Stops before code generation");
        options.addOption("c", "codegen", false, "Stops before assembly emission");

        try {
            final var commandLine = parser.parse(options, args);

            if (commandLine.getArgs().length != 1) {
                throw new ParseException("Missing file path");
            }

            final var compileOptions = new CompileOptions(
                    commandLine.hasOption("l"),
                    commandLine.hasOption("p"),
                    commandLine.hasOption("t"),
                    commandLine.hasOption("c")
            );

            final var filePath = Path.of(commandLine.getArgs()[0]);

            final var absoluteFilePath = filePath.toAbsolutePath().toString();

            final var filename = FilenameUtils.getBaseName(absoluteFilePath);
            final var fullPath = FilenameUtils.getFullPath(absoluteFilePath);

            final var preprocessedFilename = fullPath + filename + ".i";
            final var assemblyFilename = fullPath + filename + ".s";

            final var preprocessor = new ProcessBuilder();
            preprocessor.command("gcc", "-E", "-P", absoluteFilePath, "-o", preprocessedFilename);
            preprocessor.start().waitFor();

            switch (compile(preprocessedFilename, assemblyFilename, compileOptions)) {
                case Error(String message) -> {
                    System.err.println(message);
                    exit(1);
                }
                case Success() -> {
                    // Empty on purpose
                }
            }

            final var assembleAndLink = new ProcessBuilder();
            assembleAndLink.command("gcc", assemblyFilename, "-o", fullPath + filename);
            assembleAndLink.start().waitFor();
        } catch (ParseException e) {
            final var formatter = HelpFormatter.builder().get();
            formatter.printHelp("[OPTION] <FILE>", options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(e);
        }
    }

    sealed interface CompileResult {
    }

    record Success() implements CompileResult {
    }

    record Error(String message) implements CompileResult {
    }

    private static CompileResult compile(String inputPath, String outputPath, CompileOptions compileOptions) throws IOException, InterruptedException {
        var remainingFileContent = FileUtils.readFileToString(new File(inputPath), StandardCharsets.UTF_8);

        final var lexResult = new Lexer().lex(remainingFileContent);

        return handleLexerResult(compileOptions, outputPath, lexResult);
    }

    private static CompileResult handleLexerResult(CompileOptions compileOptions, String outputPath, Lexer.LexResult lexResult) {
        return switch (lexResult) {
            case Lexer.Error(char currentChar, int line, int column) ->
                    new Error("Lexer error: unexpected char = '" + currentChar + "' at line " + line + ", column " + column);
            case Lexer.Success(List<Lexer.Lexeme> lexemes) -> {
                if (compileOptions.lex) {
                    System.out.println(lexemes);

                    yield new Success();
                }

                final var parseResult = new Parser().parseProgram(lexemes);

                yield handleParserResult(compileOptions, outputPath, parseResult);
            }
        };
    }

    private static CompileResult handleParserResult(CompileOptions compileOptions, String outputPath, Parser.ParseResult parseResult) {
        return switch (parseResult) {
            case Parser.Error(List<Lexer.LexemeType> expected, Lexer.Lexeme actual) ->
                    new Error("Parser error: " + "expected one of " + formatExpectedList(expected) + ", found '" + Lexer.getDescriptorValue(actual.type()) + "', at line " + actual.line() + ", column " + actual.columnStart());
            case Parser.Success success -> {
                if (compileOptions.parse) {
                    System.out.println(success.program());

                    yield new Success();
                }

                final var tackyProgram = new TackyGen().emitTacky(success.program());

                if (compileOptions.tacky) {
                    System.out.println(tackyProgram);

                    yield new Success();
                }

                final var codegen = new Codegen().emitAssembly(tackyProgram);
                final var replacePseudo = new ReplacePseudo().emitAssembly(codegen);
                final var fixupInstructions = new FixupInstructions().emitAssembly(replacePseudo.program(), replacePseudo.stackOffset());

                yield handleCodegenResult(compileOptions,outputPath, fixupInstructions);
            }
        };
    }

    private static String formatExpectedList(List<Lexer.LexemeType> expected) {
        return expected.stream().map(Lexer::getDescriptorValue).collect(Collectors.joining("', '", "'", "'"));
    }

    private static CompileResult handleCodegenResult(CompileOptions compileOptions, String outputPath, Codegen.Program assembly) {
        if (compileOptions.codegen) {
            System.out.println(assembly);

            return new Success();
        }

        new Emitter().emit(outputPath, assembly);

        return new Success();
    }
}