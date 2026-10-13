package com.webscorer.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API contract tests: validation, status codes and the error envelope.
 * No test in this class starts a real network scan.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpointReportsUpAndLocalCopilotMode() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.copilotMode").value("local-guidance"));
    }

    @Test
    void blankUrlIsRejectedWithA400AndAMessage() throws Exception {
        mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void malformedUrlIsRejectedWithA400() throws Exception {
        mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectmapperJson("http://")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void unknownScanReturns404WithTheSharedEnvelope() throws Exception {
        mockMvc.perform(get("/api/scans/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void scanListIsEmptyForAFreshDatabase() throws Exception {
        mockMvc.perform(get("/api/scans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void copilotOnUnknownScanReturns404() throws Exception {
        mockMvc.perform(post("/api/scans/42/copilot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What should I fix first?\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void copilotRejectsBlankQuestions() throws Exception {
        mockMvc.perform(post("/api/scans/1/copilot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearingChatOnUnknownScanReturns404() throws Exception {
        mockMvc.perform(delete("/api/scans/7/copilot"))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedJsonBodyReturns400Not500() throws Exception {
        mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ------------------------------------------------------------------ CORS

    @Test
    void anyLocalhostDevPortOriginIsAllowed() throws Exception {
        // Regression: the Vite dev server moves to 5174 when 5173 is taken, and
        // that origin used to be rejected with a bare "Invalid CORS request" 403.
        mockMvc.perform(get("/api/health").header("Origin", "http://localhost:5174"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"));
    }

    @Test
    void scanCreationFromAnotherLocalhostPortPassesCors() throws Exception {
        // Reaching validation (400, not 403) proves the origin cleared CORS.
        mockMvc.perform(post("/api/scans")
                        .header("Origin", "http://localhost:5174")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"not a url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void foreignOriginsAreStillRejectedWithAJsonExplanation() throws Exception {
        mockMvc.perform(get("/api/health").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("CORS blocked")))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteIsAnAllowedCorsMethod() throws Exception {
        mockMvc.perform(options("/api/scans/1")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")));
    }

    private String objectmapperJson(String url) {
        return "{\"url\":\"" + url + "\"}";
    }
}
