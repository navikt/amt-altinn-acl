package no.nav.amt.altinn.acl.client.altinn

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

const val ALTINN3_CLIENT_ID = "altinn3"

@HttpExchange(accept = [MediaType.APPLICATION_JSON_VALUE])
interface AltinnApi {
    @PostExchange("/accessmanagement/api/v1/resourceowner/authorizedparties")
    fun hentAuthorizedParties(
        @RequestBody body: AuthorizedPartiesRequest,
    ): List<AuthorizedParty>
}
