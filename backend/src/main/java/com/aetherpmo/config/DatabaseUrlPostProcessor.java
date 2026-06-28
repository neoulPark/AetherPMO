package com.aetherpmo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 클라우드 호스트(Render/Railway/Heroku 등)가 제공하는 DATABASE_URL
 * (postgres://user:pass@host:port/db 형식)을 Spring이 이해하는
 * jdbc:postgresql://host:port/db 형식 + username/password 로 변환한다.
 *
 * SPRING_DATASOURCE_URL 이 이미 설정돼 있으면(명시적 지정) 아무것도 하지 않는다.
 * 로컬 개발에서는 DATABASE_URL 이 없으므로 application.yml 기본값을 사용한다.
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String explicit = env.getProperty("spring.datasource.url");
        String databaseUrl = env.getProperty("DATABASE_URL");

        // 이미 jdbc URL이 명시됐거나 DATABASE_URL이 없으면 변환 불필요
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (explicit != null && explicit.startsWith("jdbc:")) {
            return;
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl);
            String userInfo = uri.getUserInfo();          // user:pass
            String username = null, password = null;
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                username = parts[0];
                password = parts.length > 1 ? parts[1] : "";
            }
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
            // 클라우드 매니지드 DB는 대부분 SSL 필요
            if (uri.getQuery() != null) {
                jdbc += "?" + uri.getQuery();
            } else {
                jdbc += "?sslmode=require";
            }

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbc);
            if (username != null) props.put("spring.datasource.username", username);
            if (password != null) props.put("spring.datasource.password", password);

            env.getPropertySources().addFirst(
                    new MapPropertySource("databaseUrlDerived", props));
        } catch (Exception e) {
            // 변환 실패 시 기본 설정으로 진행 (로그만)
            System.err.println("[DatabaseUrlPostProcessor] DATABASE_URL 파싱 실패: " + e.getMessage());
        }
    }
}
