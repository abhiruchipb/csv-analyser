package com.abhiruchi.csvanalyzer.Controller;

import com.abhiruchi.csvanalyzer.entity.ColumnStats;
import com.abhiruchi.csvanalyzer.entity.FileMetadata;
import com.abhiruchi.csvanalyzer.entity.QueryHistory;
import com.abhiruchi.csvanalyzer.repository.ColumnStatsRepository;
import com.abhiruchi.csvanalyzer.repository.FileMetadataRepository;
import com.abhiruchi.csvanalyzer.repository.QueryHistoryRepository;
import com.abhiruchi.csvanalyzer.service.PythonCodeGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class UIController {

    private static final String UPLOAD_DIR = "D:/INTERVIEW_PREP/java/csvanalyzer";
    private static final String PYTHON_PATH = "C:\\\\Python312\\\\python.exe";

    private final PythonCodeGenerator codeGenerator;
    private final FileMetadataRepository fileRepo;
    private final ColumnStatsRepository statsRepo;
    private final QueryHistoryRepository queryRepo;

    public UIController(PythonCodeGenerator codeGenerator,
                        FileMetadataRepository fileRepo,
                        ColumnStatsRepository statsRepo,
                        QueryHistoryRepository queryRepo) {
        this.codeGenerator = codeGenerator;
        this.fileRepo = fileRepo;
        this.statsRepo = statsRepo;
        this.queryRepo = queryRepo;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("files", fileRepo.findAll());
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String filePath = UPLOAD_DIR + File.separator + file.getOriginalFilename();
            file.transferTo(new File(filePath));

            // Save file metadata
            FileMetadata meta = new FileMetadata();
            meta.setFilename(file.getOriginalFilename());
            meta.setFilepath(filePath);
            meta.setUploadedAt(LocalDateTime.now());
            fileRepo.save(meta);

            model.addAttribute("message", "File uploaded successfully: " + filePath);
            model.addAttribute("filePath", filePath);

            // Compute stats
            Map<String, Map<String, String>> columnStats = computeColumnStats(filePath);

            // Save stats in DB
            for (Map.Entry<String, Map<String, String>> entry : columnStats.entrySet()) {
                ColumnStats cs = new ColumnStats();
                cs.setColumnName(entry.getKey());
                cs.setColumnType(entry.getValue().get("Type"));
                cs.setCountVal(entry.getValue().get("Count"));
                cs.setMeanVal(entry.getValue().get("Mean"));
                cs.setMedianVal(entry.getValue().get("Median"));
                cs.setModeVal(entry.getValue().get("Mode"));
                cs.setMinVal(entry.getValue().get("Min"));
                cs.setMaxVal(entry.getValue().get("Max"));
                cs.setUniqueValues(entry.getValue().get("Unique Values"));
                cs.setSampleValues(entry.getValue().get("Sample Values"));
                cs.setFile(meta);
                statsRepo.save(cs);
            }

            model.addAttribute("previewRows", readPreview(filePath));
            model.addAttribute("columnStats", columnStats);
            model.addAttribute("queryHistory", queryRepo.findAll());

        } catch (IOException e) {
            model.addAttribute("message", "File upload failed: " + e.getMessage());
        }
        return "index";
    }

    @PostMapping("/ask")
    public String askQuestion(@RequestParam String question,
                              @RequestParam String filePath,
                              Model model) {
        try {
            // Generate Python code from Gemini
            String pythonCode = codeGenerator.generatePythonCode(question, filePath);
            pythonCode = pythonCode.replace("```python", "").replace("```", "").trim();

            // Save script
            File script = new File("script.py");
            try (FileWriter writer = new FileWriter(script)) {
                writer.write(pythonCode);
            }

            // Run Python
            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, "script.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            // Read output
            File outFile = new File("output.txt");
            String output = "Error: No output generated.";
            if (outFile.exists()) {
                output = new String(Files.readAllBytes(outFile.toPath()));
                model.addAttribute("result", output);
            }

            // Save query history
            FileMetadata fileMeta = fileRepo.findAll().stream()
                    .filter(f -> f.getFilepath().equals(filePath))
                    .findFirst().orElse(null);

            QueryHistory qh = new QueryHistory();
            qh.setQuestion(question);
            qh.setAnswer(output);
            qh.setAskedAt(LocalDateTime.now());
            qh.setFile(fileMeta);
            queryRepo.save(qh);

            // Keep preview + stats visible
            model.addAttribute("previewRows", readPreview(filePath));
            model.addAttribute("columnStats", computeColumnStats(filePath));
            model.addAttribute("queryHistory", queryRepo.findAll());

        } catch (Exception e) {
            model.addAttribute("result", "Error: " + e.getMessage());
        }

        model.addAttribute("filePath", filePath);
        return "index";
    }

    // 🔹 Helper: Preview top 5 rows
    private List<List<String>> readPreview(String filePath) throws IOException {
        List<List<String>> previewRows = new ArrayList<>();
        try (Reader reader = new FileReader(filePath);
             org.apache.commons.csv.CSVParser csvParser =
                     new org.apache.commons.csv.CSVParser(reader,
                             org.apache.commons.csv.CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            List<String> headers = new ArrayList<>(csvParser.getHeaderMap().keySet());
            previewRows.add(headers);

            int count = 0;
            for (org.apache.commons.csv.CSVRecord record : csvParser) {
                List<String> row = new ArrayList<>();
                for (String header : headers) {
                    row.add(record.get(header));
                }
                previewRows.add(row);
                count++;
                if (count >= 5) break;
            }
        }
        return previewRows;
    }

    // 🔹 Helper: Compute stats + column types
    private Map<String, Map<String, String>> computeColumnStats(String filePath) throws IOException {
        Map<String, List<String>> columnData = new LinkedHashMap<>();

        try (Reader reader = new FileReader(filePath);
             org.apache.commons.csv.CSVParser csvParser =
                     new org.apache.commons.csv.CSVParser(reader,
                             org.apache.commons.csv.CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            List<String> headers = new ArrayList<>(csvParser.getHeaderMap().keySet());
            for (String header : headers) {
                columnData.put(header, new ArrayList<>());
            }

            for (org.apache.commons.csv.CSVRecord record : csvParser) {
                for (String header : headers) {
                    columnData.get(header).add(record.get(header));
                }
            }
        }

        Map<String, Map<String, String>> stats = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : columnData.entrySet()) {
            String col = entry.getKey();
            List<String> values = entry.getValue();
            Map<String, String> colStats = new LinkedHashMap<>();

            // Detect type
            String type = detectColumnType(values);
            colStats.put("Type", type);

            if (type.equals("Numeric")) {
                List<Double> nums = new ArrayList<>();
                for (String v : values) {
                    try {
                        nums.add(Double.parseDouble(v));
                    } catch (Exception ignored) {}
                }
                if (!nums.isEmpty()) {
                    double count = nums.size();
                    double mean = nums.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double min = nums.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                    double max = nums.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    double median = nums.stream().sorted().skip(nums.size() / 2).findFirst().orElse(0.0);
                    double mode = nums.stream()
                            .reduce((a, b) -> Collections.frequency(nums, a) > Collections.frequency(nums, b) ? a : b)
                            .orElse(0.0);

                    colStats.put("Count", String.valueOf((int) count));
                    colStats.put("Mean", String.format("%.2f", mean));
                    colStats.put("Median", String.format("%.2f", median));
                    colStats.put("Mode", String.format("%.2f", mode));
                    colStats.put("Min", String.format("%.2f", min));
                    colStats.put("Max", String.format("%.2f", max));
                }
            } else {
                // For categorical/date: just count unique values
                Set<String> unique = new HashSet<>(values);
                colStats.put("Unique Values", String.valueOf(unique.size()));
                colStats.put("Sample Values", String.join(", ", unique.stream().limit(3).toList()));
            }

            stats.put(col, colStats);
        }

        return stats;
    }

    // 🔹 Helper: Detect column type
    private String detectColumnType(List<String> values) {
        int numericCount = 0, dateCount = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        for (String v : values) {
            if (v == null || v.isBlank()) continue;
            try {
                Double.parseDouble(v);
                numericCount++;
            } catch (Exception ignored) {}
            try {
                sdf.parse(v);
                dateCount++;
            } catch (ParseException ignored) {}
        }

        if (numericCount > values.size() * 0.8) return "Numeric";
        if (dateCount > values.size() * 0.8) return "Date";
        return "Categorical";
    }
}