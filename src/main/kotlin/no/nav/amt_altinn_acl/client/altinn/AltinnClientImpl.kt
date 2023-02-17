package no.nav.amt_altinn_acl.client.altinn

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.amt_altinn_acl.utils.JsonUtils.fromJsonString
import no.nav.amt_altinn_acl.utils.SecureLog.secureLog
import no.nav.common.rest.client.RestClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory

class AltinnClientImpl(
	private val baseUrl: String,
	private val altinnApiKey: String,
	private val maskinportenTokenProvider: () -> String,
	private val client: OkHttpClient = RestClient.baseClient(),
) : AltinnClient {
	private val log = LoggerFactory.getLogger(javaClass)

	override fun hentOrganisasjoner(norskIdent: String, serviceCode: String): Result<List<String>> {
		val request = Request.Builder()
			.url("$baseUrl/api/serviceowner/reportees?subject=$norskIdent&serviceCode=$serviceCode&serviceEdition=1")
			.addHeader("APIKEY", altinnApiKey)
			.addHeader("Authorization", "Bearer ${maskinportenTokenProvider.invoke()}")
			.get()
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				secureLog.error("Klarte ikke å hente organisasjoner for serviceCode=$serviceCode norskIdent=$norskIdent message=${response.message}, code=${response.code}, body=${response.body?.string()}")
				log.error("Klarte ikke hente organisasjoner for $serviceCode. response: ${response.code}")
				return Result.failure(RuntimeException("Klarte ikke å hente organisasjoner code=${response.code}"))
			}

			val body = response.body?.string()
				?: return Result.failure(Exception("Body is missing"))

			val data = fromJsonString<List<ReporteeResponseEntity.Reportee>>(body)
				.filter { it.organisasjonsnummer != null }
				.mapNotNull { it.organisasjonsnummer }

			return Result.success(data)
		}
	}

	object ReporteeResponseEntity {
		data class Reportee(
			@JsonAlias("Type")
			val type: String,

			@JsonAlias("OrganizationNumber")
			val organisasjonsnummer: String?,
		)
	}
}
