package com.abhiruchi.csvanalyzer.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.stereotype.Service;

@Service
public class PythonCodeGenerator {

    private final ChatLanguageModel model;

    public PythonCodeGenerator() {
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY")) // or load from application.properties
                .modelName("gemini-1.5-flash") // or gemini-pro
                .build();
    }

    public String generatePythonCode(String question, String filePath) {
        String prompt = """
            You are a helpful assistant that generates Python code to analyze CSV files.
            - Always use pandas for data analysis.
            - DO NOT generate charts, plots, or images.
            - DO NOT use matplotlib or seaborn.
            - Always write the final answer into 'output.txt'.
            - Do not print to console, only write to 'output.txt'.
            - The answer must be plain text, not a chart reference.

            Question: %s
            CSV file path: %s
        """.formatted(question, filePath);

        return model.generate(prompt);
    }
}