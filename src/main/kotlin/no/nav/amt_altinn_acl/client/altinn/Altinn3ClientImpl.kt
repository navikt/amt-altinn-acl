package no.nav.amt_altinn_acl.client.altinn

import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.utils.JsonUtils
import no.nav.amt_altinn_acl.utils.JsonUtils.fromJsonString
import no.nav.amt_altinn_acl.utils.SecureLog.secureLog
import no.nav.common.rest.client.RestClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory


class Altinn3ClientImpl(
	private val baseUrl: String,
	private val maskinportenTokenProvider: () -> String,
	private val client: OkHttpClient = RestClient.baseClient(),
) : AltinnClient {
	private val log = LoggerFactory.getLogger(javaClass)

	override fun hentAlleOrganisasjoner(norskIdent: String, roller: List<RolleType>): Map<RolleType, List<String>> {
		val parties = hentAuthorizedParties(norskIdent)
		val resourceIds = roller.map { it.resourceId }.toSet()


		return roller.associateWith { rolle ->
			parties.flatMap { it.finnTilganger(resourceIds) }
				.filter { it.rolle == rolle }
				.map { it.organisasjonsnummer }
		}.also {
			log.info("Hentet ${it.values.sumOf { it.size }} $roller tilganger fra Altinn 3")
		}
	}

	private fun hentAuthorizedParties(norskIdent: String): List<AuthorizedParty> {
		val request = Request.Builder()
			.url("$baseUrl/accessmanagement/api/v1/resourceowner/authorizedparties")
			.addHeader("Authorization", "Bearer ${maskinportenTokenProvider.invoke()}")
			.post(requestBody(norskIdent))
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				secureLog.error("Klarte ikke å hente organisasjoner for norskIdent=$norskIdent message=${response.message}, code=${response.code}, body=${response.body?.string()}")
				log.error("Klarte ikke hente organisasjoner ${response.code}")
				throw RuntimeException("Klarte ikke å hente organisasjoner code=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			return fromJsonString<List<AuthorizedParty>>(body)
		}
	}

	private fun requestBody(norskIdent: String) = JsonUtils
		.objectMapper
		.writeValueAsString(AuthorizedPartiesRequest(norskIdent))
		.toRequestBody("application/json".toMediaType())

	data class AuthorizedPartiesRequest(
		val value: String,
		val type: String = "urn:altinn:person:identifier-no",
	)

	data class AuthorizedParty(
		val organizationNumber: String?,
		val authorizedResources: Set<String>,
		val subunits: List<AuthorizedParty>,
	) {
		fun finnTilganger(resourceIds: Set<String>): List<Tilgang> {
			val tilganger = organizationNumber
				?.let {
					authorizedResources
						.intersect(resourceIds)
						.map { Tilgang(RolleType.fromResourceId(it), organizationNumber) }
				}
				?: emptyList()

			val underenhetTilganger = subunits.flatMap { it.finnTilganger(resourceIds) }

			return tilganger + underenhetTilganger
		}
	}

	data class Tilgang(
		val rolle: RolleType,
		val organisasjonsnummer: String,
	)
}
