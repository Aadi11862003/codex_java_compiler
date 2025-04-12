package com.example.javacompiler.controller;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compiler")
public class CompilerController {

    @PostMapping("/compile")
    public Map<String, Object> compileAndRun(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String input = request.getOrDefault("input", ""); // Custom input for the program
        Map<String, Object> response = new HashMap<>();

        try {
            // Save code to a temporary file
            File sourceFile = new File("Main.java");
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(code);
            }

            // Compile the code
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFile);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

            boolean success = task.call();
            fileManager.close();

            if (!success) {
                StringBuilder errorMessages = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errorMessages.append("Error on line ").append(diagnostic.getLineNumber())
                            .append(": ").append(diagnostic.getMessage(null)).append("\n");
                }
                response.put("success", false);
                response.put("message", "Compilation failed");
                response.put("errors", errorMessages.toString());
                return response;
            }

            // Run the compiled code
            ProcessBuilder processBuilder = new ProcessBuilder("java", "Main");
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            Process process = processBuilder.start();

            // Provide custom input to the program
            if (!input.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write(input);
                    writer.flush();
                }
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                response.put("success", false);
                response.put("message", "Runtime error occurred");
                response.put("output", output.toString());
            } else {
                response.put("success", true);
                response.put("output", output.toString());
            }

            // Analyze the code for time and space complexity
            Map<String, String> complexity = analyzeComplexity(code);
            response.put("timeComplexity", complexity.get("timeComplexity"));
            response.put("spaceComplexity", complexity.get("spaceComplexity"));

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error during compilation or execution");
            response.put("error", e.getMessage());
        }

        return response;
    }

    private Map<String, String> analyzeComplexity(String code) {
        Map<String, String> complexity = new HashMap<>();
        // Basic heuristic for time and space complexity analysis
        if (code.contains("for") || code.contains("while")) {
            complexity.put("timeComplexity", "O(n)");
        } else {
            complexity.put("timeComplexity", "O(1)");
        }

        if (code.contains("new")) {
            complexity.put("spaceComplexity", "O(n)");
        } else {
            complexity.put("spaceComplexity", "O(1)");
        }

        return complexity;
    }
}