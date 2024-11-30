package com.github.bechernie;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        final var parser = new DefaultParser();

        final var options = new Options();
        options.addOption("l", "lex", false, "Stops before parsing");
        options.addOption("p", "parse", false, "Stops before assembly generation");
        options.addOption("c", "codegen", false, "Stops before code emission");

        try {
            final var commandLine = parser.parse(options, args);

            if (commandLine.getArgs().length != 1) {
                throw new ParseException("Missing file path");
            }

            final var filePath = Path.of(commandLine.getArgs()[0]);

            final var absoluteFilePath = filePath.toAbsolutePath().toString();

            final var filename = FilenameUtils.getBaseName(absoluteFilePath);
            final var fullPath = FilenameUtils.getFullPath(absoluteFilePath);

            final var preprocessedFilename = fullPath + filename + ".i";
            final var assemblyFilename = fullPath + filename + ".s";

            final var preprocessor = new ProcessBuilder();
            preprocessor.command("gcc", "-E", "-P", absoluteFilePath, "-o", preprocessedFilename);
            preprocessor.start().waitFor();

            compile(preprocessedFilename, assemblyFilename);

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

    private static void compile(String inputPath, String outputPath) throws IOException, InterruptedException {
        final var mockCompile = new ProcessBuilder();
        mockCompile.command("gcc", "-S", "-O", "-fno-asynchronous-unwind-tables", "-fcf-protection=none", inputPath, "-o", outputPath);
        mockCompile.start().waitFor();
    }
}