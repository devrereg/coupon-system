package kr.co.mindrepublic.coupon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Day 1 검증: spring.threads.virtual.enabled=true 설정으로
 * 톰캣이 HTTP 요청을 가상 스레드(Virtual Thread)에서 처리하는지 확인한다.
 *
 * 실제 서버를 띄우고(RANDOM_PORT) 테스트 전용 컨트롤러로 왕복 요청을 보낸 뒤,
 * 요청을 처리한 스레드가 가상 스레드인지(Thread.isVirtual) 검사한다.
 *
 * NOTE: @SpringBootTest는 전체 컨텍스트를 로드하므로 PostgreSQL/Redis가 떠 있어야 한다.
 *       먼저 `docker compose up -d`로 인프라를 기동할 것.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VirtualThreadSmokeTest.ThreadProbeController::class)
class VirtualThreadSmokeTest {

	@LocalServerPort
	var port: Int = 0

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Test
	fun `HTTP 요청은 가상 스레드에서 처리된다`() {
		val body = restTemplate.getForObject(
			"http://localhost:$port/__smoke/thread",
			String::class.java,
		)
		// 응답 형식: "virtual=true,name=..."
		assertThat(body).startsWith("virtual=true")
	}

	@RestController
	class ThreadProbeController {
		@GetMapping("/__smoke/thread")
		fun thread(): String {
			val t = Thread.currentThread()
			return "virtual=${t.isVirtual},name=${t.name}"
		}
	}
}
