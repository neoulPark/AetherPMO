package com.aetherpmo.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 공개 헬스체크 엔드포인트 (인증 불필요).
 * Render 등 호스트의 헬스체크가 200을 받도록 한다.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
