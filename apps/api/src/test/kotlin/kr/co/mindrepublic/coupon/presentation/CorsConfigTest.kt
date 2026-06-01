package kr.co.mindrepublic.coupon.presentation

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.options

/**
 * CORS 설정 검증: 프론트 출처(http://localhost:5173)의 `/api/` 하위 preflight 가 허용되는지.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `5173 출처의 POST preflight 를 허용한다`() {
        mockMvc.options("/api/coupons/1/issue") {
            header(HttpHeaders.ORIGIN, "http://localhost:5173")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173") }
        }
    }

    @Test
    fun `5173 출처의 GET preflight 를 허용한다`() {
        mockMvc.options("/api/coupons") {
            header(HttpHeaders.ORIGIN, "http://localhost:5173")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173") }
        }
    }
}
