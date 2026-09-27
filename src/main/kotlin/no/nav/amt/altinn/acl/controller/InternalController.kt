package no.nav.amt.altinn.acl.controller

import no.nav.amt.altinn.acl.service.RolleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class InternalController(
    private val rolleService: RolleService,
) {
    @GetMapping("/altinn/synkroniser")
    fun synkroniserAltinnRettigheter() = rolleService.synchronizeUsers()
}
