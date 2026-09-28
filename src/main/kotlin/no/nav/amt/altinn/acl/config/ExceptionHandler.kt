package no.nav.amt.altinn.acl.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Response> = when (e) {
        is NoSuchElementException -> buildResponse(HttpStatus.NOT_FOUND, e)
        is IllegalArgumentException -> buildResponse(HttpStatus.BAD_REQUEST, e)
        else -> buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e)
    }

    private fun buildResponse(
        status: HttpStatus,
        e: Exception,
    ): ResponseEntity<Response> {
        if (status.is5xxServerError) {
            log.error("Uventet feil under behandling av forespørsel", e)
        } else {
            log.info(e.message, e)
        }

        return ResponseEntity.status(status).body(
            Response(
                status = status.value(),
                title = status,
                detail = if (status.is5xxServerError) "En uventet feil oppstod" else e.message,
            ),
        )
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Response(
        val status: Int,
        val title: HttpStatus,
        val detail: String?,
    )
}
