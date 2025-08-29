package com.kanishk.simpleWebApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class SimpleWebAppApplication {

    private static final Logger log = LoggerFactory.getLogger(SimpleWebAppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SimpleWebAppApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(ChallengeService challengeService) {
        return args -> {
            log.info("Application started, beginning the hiring challenge process...");
            challengeService.solveChallenge();
            log.info("Hiring challenge process completed. Shutting down.");
        };
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}

@Service
class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);
    private final RestTemplate restTemplate;

    private static final String GENERATE_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    
    public ChallengeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void solveChallenge() {
        try {
            WebhookResponse webhookResponse = generateWebhook();
            if (webhookResponse == null || webhookResponse.getWebhook() == null || webhookResponse.getAccessToken() == null) {
                log.error("Failed to retrieve webhook URL or access token. Aborting.");
                return;
            }
            log.info("Successfully received webhook URL: {}", webhookResponse.getWebhook());
            log.info("Successfully received access token.");

            String finalQuery = getFinalSqlQuery();
            log.info("Final SQL query formulated:\n{}", finalQuery);

            submitSolution(webhookResponse.getWebhook(), webhookResponse.getAccessToken(), finalQuery);

        } catch (HttpClientErrorException e) {
            log.error("An HTTP error occurred: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("An unexpected error occurred during the challenge process.", e);
        }
    }

    private WebhookResponse generateWebhook() {
        log.info("Step 1: Sending POST request to generate webhook...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        WebhookRequest requestBody = new WebhookRequest("Shivam Dua", "22BCE2466", "shivam.dua2022@vitstudent.ac.in");

        HttpEntity<WebhookRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
            GENERATE_WEBHOOK_URL,
            requestEntity,
            WebhookResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            log.error("Failed to generate webhook. Status code: {}", response.getStatusCode());
            return null;
        }
    }

    private String getFinalSqlQuery() {
        log.info("Step 3: Solving SQL problem for even registration number (22BCE2466)...");
        return "SELECT " +
           "  e1.EMP_ID, " +
           "  e1.FIRST_NAME, " +
           "  e1.LAST_NAME, " +
           "  d.DEPARTMENT_NAME, " +
           "  COALESCE(COUNT(e2.EMP_ID), 0) AS YOUNGER_EMPLOYEES_COUNT " +
           "FROM EMPLOYEE e1 " +
           "LEFT JOIN DEPARTMENT d " +
           "  ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
           "LEFT JOIN EMPLOYEE e2 " +
           "  ON e1.DEPARTMENT = e2.DEPARTMENT " +
           "  AND e2.DOB > e1.DOB " +
           "GROUP BY " +
           "  e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
           "ORDER BY e1.EMP_ID DESC;";

    }

    private void submitSolution(String webhookUrl, String accessToken, String finalQuery) {
        log.info("Step 4: Submitting the final solution to the webhook...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        SolutionRequest requestBody = new SolutionRequest(finalQuery);

        HttpEntity<SolutionRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            webhookUrl,
            requestEntity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Successfully submitted the solution! Server response: {}", response.getBody());
        } else {
            log.error("Failed to submit the solution. Status code: {}", response.getStatusCode());
            log.error("Response body: {}", response.getBody());
        }
    }
}

class WebhookRequest {
    private String name;
    private String regNo;
    private String email;

    public WebhookRequest() {
    }

    public WebhookRequest(String name, String regNo, String email) {
        this.name = name;
        this.regNo = regNo;
        this.email = email;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class WebhookResponse {
    private String webhook;
    private String accessToken;
    
    public WebhookResponse() {
    }

    public String getWebhook() { return webhook; }
    public void setWebhook(String webhook) { this.webhook = webhook; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}

class SolutionRequest {
    private String finalQuery;

    public SolutionRequest() {
    }

    public SolutionRequest(String finalQuery) {
        this.finalQuery = finalQuery;
    }

    public String getFinalQuery() { return finalQuery; }
    public void setFinalQuery(String finalQuery) { this.finalQuery = finalQuery; }
}
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
// public class SimpleWebAppApplication {

// 	public static void main(String[] args) {
// 		SpringApplication.run(SimpleWebAppApplication.class, args);
// 	}

// }
