package com.coderunner.coderunner;


import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Controller
public class HomeController {

    private final String clientId = "3357c5bb92548c28d2fafd37e8210fb4";
    private final String clientSecret = "c4471261bb267e5b12b681ed501bcde27f101bb93c3e4931a2a0b1d533a8e54b";

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "CodeRunner");
        return "index";
    }

    @PostMapping("/run")
public String runCode(@RequestParam String language, @RequestParam String code, @RequestParam(required = false) String input, Model model) {
    String versionIndex = "0";

    if ((language.equals("java") && code.contains("Scanner") && (input == null || input.isBlank())) ||
        (language.equals("python3") && code.contains("input(") && (input == null || input.isBlank())) ||
        (language.equals("c") && code.contains("scanf") && (input == null || input.isBlank())) ||
        (language.equals("cpp") && code.contains("cin") && (input == null || input.isBlank()))) {

        model.addAttribute("language", language);
        model.addAttribute("code", code);
        model.addAttribute("input", input);
        model.addAttribute("output", "⚠️ Your code is expecting input but none was provided. Please enter input and re-run the code.");
        return "result";
    }

    JDoodleRequest request = new JDoodleRequest();
    request.setScript(code);
    request.setLanguage(language);
    request.setVersionIndex(versionIndex);
    request.setClientId(clientId);
    request.setClientSecret(clientSecret);
    request.setStdin(input);

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<JDoodleRequest> entity = new HttpEntity<>(request, headers);

    try {
        ResponseEntity<JDoodleResponse> response = restTemplate.exchange(
            "https://api.jdoodle.com/v1/execute",
            HttpMethod.POST,
            entity,
            JDoodleResponse.class
        );

        // Combine input prompts and inputs
        StringBuilder combinedInputs = new StringBuilder();
        String[] inputs = input != null ? input.split("\\r?\\n") : new String[0];
        String[] codeLines = code.split("\\r?\\n");
        int inputIndex = 0;

        for (String line : codeLines) {
            line = line.trim();
            if ((line.startsWith("System.out.println") || line.startsWith("System.out.print")) && inputIndex < inputs.length) {
                int start = line.indexOf("\"");
                int end = line.lastIndexOf("\"");
                if (start >= 0 && end > start) {
                    String prompt = line.substring(start + 1, end);
                    combinedInputs.append(prompt).append(" ").append(inputs[inputIndex]).append("\n");
                    inputIndex++;
                }
            }
            if (inputIndex >= inputs.length) break;
        }

        String jdoodleOutput = response.getBody().getOutput();

        String[] promptLines = combinedInputs.toString().split("\\r?\\n");
        for (String prompt : promptLines) {
            String promptOnly = prompt.contains(":") ? prompt.substring(0, prompt.indexOf(":") + 1) : prompt;
            jdoodleOutput = jdoodleOutput.replace(promptOnly, ""); // remove "Enter your name:" etc
        }

        // Final clean output
        String fullOutput = combinedInputs.toString().trim() + "\n\n" + jdoodleOutput.trim();
        model.addAttribute("output", fullOutput);


        model.addAttribute("language", language);
        model.addAttribute("code", code);
        model.addAttribute("input", input);
        model.addAttribute("output", fullOutput);

    } catch (Exception e) {
        model.addAttribute("language", language);
        model.addAttribute("code", code);
        model.addAttribute("input", input);
        model.addAttribute("output", "An error occurred: " + e.getMessage());
    }

    return "result";
}

}
