package no.nav.amt.altinn.acl.client.altinn

import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

const val ALTINN3_CLIENT_ID = "altinn3"

@HttpExchange
@ClientRegistrationId(ALTINN3_CLIENT_ID)
interface AltinnApi {
    @PostExchange("/accessmanagement/api/v1/resourceowner/authorizedparties")
    fun hentAuthorizedParties(
        @RequestBody body: AuthorizedPartiesRequest,
    ): List<AuthorizedParty>
}
