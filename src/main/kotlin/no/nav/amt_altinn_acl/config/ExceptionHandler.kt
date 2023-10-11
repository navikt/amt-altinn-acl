package no.nav.amt_altinn_acl.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class ExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(NoSuchElementException::class)
	fun handleNotFoundException(e: NoSuchElementException): ResponseEntity<Response> {
		log.info(e.message, e)
		val status = HttpStatus.NOT_FOUND

		return ResponseEntity
			.status(status)
			.body(
				Response(
					status = status.value(),
					title = status,
					detail = e.message,
				)
			)
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(IllegalArgumentException::class)
	fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<Response> {
		log.info(e.message, e)
		val status = HttpStatus.BAD_REQUEST

		return ResponseEntity
			.status(status)
			.body(
				Response(
					status = status.value(),
					title = status,
					detail = e.message,
				)
			)
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	data class Response(
		val status: Int,
		val title: HttpStatus,
		val detail: String?,
	)
}
