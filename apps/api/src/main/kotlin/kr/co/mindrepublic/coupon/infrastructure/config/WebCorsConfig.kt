package kr.co.mindrepublic.coupon.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * CORS 설정(기술 세부사항 → infrastructure).
 *
 * 프론트(Vite dev 서버, http://localhost:5173)가 백엔드(http://localhost:8080)를 직접 호출한다.
 * `/api/` 하위 전체에 대해 GET/POST + preflight(OPTIONS)를 허용한다.
 * (Vite proxy 대신 CORS 방식을 학습 목적상 직접 다룬다 — 플랜 단위 1 결정사항)
 */
@Configuration
class WebCorsConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "OPTIONS")
    }
}
