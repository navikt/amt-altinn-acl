package no.nav.amt.altinn.acl.client.altinn

import no.nav.amt.altinn.acl.client.MaskinportenTokenClient
import no.nav.amt.altinn.acl.domain.RolleType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException

@Service
class Altinn3Client(
    private val altinnApi: AltinnApi,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun hentRoller(
        norskIdent: String,
        roller: List<RolleType>,
    ): Map<RolleType, List<String>> {
        val parties = hentAuthorizedParties(norskIdent)
        val resourceIds = roller.map { it.resourceId }.toSet()

        return roller
            .associateWith { rolle ->
                parties
                    .flatMap { it.finnTilganger(resourceIds) }
                    .filter { it.rolle == rolle }
                    .map { it.organisasjonsnummer }
            }.also {
                log.info("Hentet ${it.values.sumOf { strings -> strings.size }} $roller tilganger fra Altinn 3")
            }
    }

    private fun hentAuthorizedParties(norskIdent: String): List<AuthorizedParty> = try {
        altinnApi.hentAuthorizedParties(AuthorizedPartiesRequest(norskIdent))
    } catch (e: RestClientResponseException) {
        throw AltinnClientException(
            statusCode = e.statusCode.value(),
        )
    } catch (e: MaskinportenTokenClient.MaskinportenTokenException) {
        throw AltinnClientException(
            statusCode = e.statusCode,
            errorCode = e.errorCode,
        )
    }

    class AltinnClientException(
        val statusCode: Int,
        val errorCode: String? = null,
    ) : RuntimeException("Klarte ikke å hente organisasjoner fra Altinn, status=$statusCode")
}
