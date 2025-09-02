package com.abhiruchi.csv_analyzer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DataAnalysisController {

    private final DataAnalysisAgent dataAnalysisAgent;

    public DataAnalysisController(DataAnalysisAgent dataAnalysisAgent) {
        this.dataAnalysisAgent = dataAnalysisAgent;
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt) {

        // For now, we just log the received file info
        System.out.println("Received file: " + file.getOriginalFilename());

        // Call the AI agent to generate the script
        String pythonScript = dataAnalysisAgent.generatePythonScript(prompt);

        // Return the generated script as the response
        return ResponseEntity.ok(pythonScript);
    }
}