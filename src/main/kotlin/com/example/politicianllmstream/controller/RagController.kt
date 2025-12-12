package com.example.politicianllmstream.controller

import com.example.politicianllmstream.dto.RagRequest
import com.example.politicianllmstream.service.RagLogService
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
    private val ragLogService: RagLogService,
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

        val ip = extractClientIp(exchange)

        return rateLimitService.check(ip)
            .flatMapMany { allowed ->
                if (!allowed) {
                    Flux.just("1시간에 10회까지만 질의가 가능합니다.")
                } else {
                    val stream = ragService.askStream(req)
                    val saveLogMono = ragLogService.saveLog(ip, req.query)

                    stream.concatWith(
                        saveLogMono.thenMany(Flux.empty())
                    )
                }
            }
            .onErrorResume { e ->
                // 클라이언트에게 메시지 보내기
                Flux.just("응답에 지연이 생기고 있습니다. 잠시후에 다시 시도해주세요. ${e.message}")
            }
    }

    fun extractClientIp(exchange: ServerWebExchange): String {
        val headers = exchange.request.headers

        val forwardedFor = headers.getFirst("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",")[0].trim()
        }

        val realIp = headers.getFirst("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }

        return exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
    }
}
