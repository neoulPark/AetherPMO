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
 * 우선순위:
 *  1) SPRING_DATASOURCE_URL 환경변수가 명시돼 있으면 그것을 사용 (변환 안 함)
 *  2) DATABASE_URL 환경변수가 있으면 변환하여 사용 (application.yml 기본값보다 우선)
 *  3) 둘 다 없으면 application.yml 기본값(localhost) 사용 — 로컬 개발
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        // 명시적 SPRING_DATASOURCE_URL(환경변수)이 있으면 그대로 둔다.
        // 주의: application.yml 기본값(jdbc:localhost)은 무시해야 하므로
        //      env.getProperty(...) 가 아니라 실제 OS 환경변수만 본다.
        String explicitEnv = System.getenv("SPRING_DATASOURCE_URL");
        if (explicitEnv != null && !explicitEnv.isBlank()) {
            return;
        }

        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            // 로컬 개발: application.yml 기본값 사용
            System.out.println("[DatabaseUrlPostProcessor] DATABASE_URL 없음 → application.yml 기본값 사용");
            return;
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            System.out.println("[DatabaseUrlPostProcessor] DATABASE_URL 형식이 postgres:// 아님 → 변환 생략");
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
            // sslmode=prefer: SSL 지원 시 SSL 사용, 미지원 시 평문 — 내부/외부 모두 안전
            jdbc += (uri.getQuery() != null) ? "?" + uri.getQuery() : "?sslmode=prefer";

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbc);
            if (username != null) props.put("spring.datasource.username", username);
            if (password != null) props.put("spring.datasource.password", password);

            // addFirst → application.yml 기본값보다 우선 적용
            env.getPropertySources().addFirst(
                    new MapPropertySource("databaseUrlDerived", props));

            System.out.println("[DatabaseUrlPostProcessor] DATABASE_URL 변환 완료 → "
                    + "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath() + " (user=" + username + ")");
        } catch (Exception e) {
            System.err.println("[DatabaseUrlPostProcessor] DATABASE_URL 파싱 실패: " + e.getMessage());
        }
    }
}
