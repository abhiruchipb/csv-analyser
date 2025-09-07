package com.abhiruchi.csvanalyzer.Controller;

import com.abhiruchi.csvanalyzer.service.PythonCodeGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private static final String PYTHON_PATH = "C:\\\\Python312\\\\python.exe"; // Adjust if needed

    private final PythonCodeGenerator codeGenerator;

    public QueryController(PythonCodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    @PostMapping("/ask")
    public ResponseEntity<String> askQuestion(
            @RequestParam String question,
            @RequestParam String filePath) {

        try {
            // Step 1: Ask Gemini to generate Python code dynamically
            String pythonCode = codeGenerator.generatePythonCode(question, filePath);

            // Debug: log the generated code
            System.out.println("Generated Python code:\n" + pythonCode);
            pythonCode = pythonCode
                .replace("```python", "")
                .replace("```", "")
                .trim();
            // Step 2: Save Python script
            File script = new File("script.py");
            try (FileWriter writer = new FileWriter(script)) {
                writer.write(pythonCode);
            }

            // Step 3: Run Python script
            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, "script.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder consoleOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                consoleOutput.append(line).append("\n");
            }
            process.waitFor();

            // Step 4: Read output
            File outFile = new File("output.txt");
            if (!outFile.exists()) {
                return ResponseEntity.status(500).body("Python error:\n" + consoleOutput);
            }

            String output = new String(java.nio.file.Files.readAllBytes(outFile.toPath()));

            return ResponseEntity.ok(output);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}