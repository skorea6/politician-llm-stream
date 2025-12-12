package com.example.politicianllmstream.controller

import com.example.politicianllmstream.dto.RagRequest
import com.example.politicianllmstream.service.RagService
import com.example.politicianllmstream.service.RateLimitService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/ai")
class RagController(
    private val ragService: RagService,
    private val rateLimitService: RateLimitService
) {

    @PostMapping(
        "/stream",
        produces = [MediaType.TEXT_PLAIN_VALUE]   // SSE
    )
    fun answer(
        @RequestBody req: RagRequest,
        exchange: ServerWebExchange
    ): Flux<String> {

        val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"

        return rateLimitService.check(ip)
            .flatMapMany { allowed ->
                if (!allowed) {
                    Flux.just("1시간에 10회까지만 질의가 가능합니다.")
                } else {
                    ragService.askStream(req)
                }
            }
//            .onErrorResume { // SSE용 에러 처리
//                Flux.just("응답에 지연이 생기고 있습니다. 잠시후에 다시 시도해주세요.")
//            }
            .onErrorResume { e ->
                // 로그 남기기
                print(e)
                print(e.message)

                // 클라이언트에게 메시지 보내기
                Flux.just("응답에 지연이 생기고 있습니다. 잠시후에 다시 시도해주세요. ${e.message}")
            }
    }
}
