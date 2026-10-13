package com.webscorer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * CORS for the local Vite dev server plus the small, bounded thread pool that
 * runs scans off the request thread (the API stays synchronous per request,
 * the scan itself reports its real stage through the database).
 *
 * <p>The Vite dev server auto-increments its port when 5173 is taken (5174, ...),
 * and its proxy sets {@code changeOrigin}, so the backend sees the browser's
 * {@code Origin} header on every state-changing request. Any localhost port is
 * therefore allowed as a dev origin; requests from other origins are rejected
 * with the same JSON envelope as the rest of the API.</p>
 */
@Configuration
@EnableAsync
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    /** Always allowed: the dev server can land on any localhost port. */
    private static final String[] DEV_ORIGIN_PATTERNS = {
            "http://localhost:*",
            "http://127.0.0.1:*"
    };

    @Value("${webscorer.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>();
        for (String origin : allowedOrigins.split(",")) {
            if (!origin.isBlank()) {
                origins.add(origin.trim());
            }
        }
        origins.addAll(List.of(DEV_ORIGIN_PATTERNS));
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /**
     * Rejected CORS requests answer with the shared JSON error envelope instead
     * of Spring's plain-text "Invalid CORS request", so the frontend can show
     * the real reason for the 403.
     */
    @Bean
    public static BeanPostProcessor corsRejectingProcessorCustomizer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof RequestMappingHandlerMapping mapping) {
                    mapping.setCorsProcessor(new JsonRejectCorsProcessor());
                }
                return bean;
            }
        };
    }

    static class JsonRejectCorsProcessor extends DefaultCorsProcessor {

        private static final Logger corsLog = LoggerFactory.getLogger(JsonRejectCorsProcessor.class);
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        protected void rejectRequest(ServerHttpResponse response) throws IOException {
            if (!(response instanceof ServletServerHttpResponse servletResponse)) {
                super.rejectRequest(response);
                return;
            }
            String origin = firstHeader("Origin");
            String path = requestPath();
            corsLog.warn("CORS rejected origin '{}' for {}", origin, path);
            String message = "CORS blocked this request: origin "
                    + (origin == null || origin.isBlank() ? "(unknown)" : origin)
                    + " is not allowed. Only the WebScorer frontend may call this API "
                    + "(allowed origins are set with webscorer.cors.allowed-origins).";
            servletResponse.getServletResponse().setStatus(HttpStatus.FORBIDDEN.value());
            servletResponse.getServletResponse().setContentType("application/json");
            servletResponse.getServletResponse().setCharacterEncoding("UTF-8");
            servletResponse.getServletResponse().getWriter().write(mapper.writeValueAsString(Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", HttpStatus.FORBIDDEN.value(),
                    "error", "Forbidden",
                    "message", message,
                    "path", path)));
        }

        private String firstHeader(String name) {
            jakarta.servlet.http.HttpServletRequest request = request();
            return request == null ? null : request.getHeader(name);
        }

        private String requestPath() {
            jakarta.servlet.http.HttpServletRequest request = request();
            return request == null ? "/api" : request.getRequestURI();
        }

        private jakarta.servlet.http.HttpServletRequest request() {
            org.springframework.web.context.request.RequestAttributes attrs =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            return attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttrs
                    ? servletAttrs.getRequest()
                    : null;
        }
    }

    @Bean(name = "scanExecutor")
    public Executor scanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("scan-");
        executor.initialize();
        return executor;
    }
}
