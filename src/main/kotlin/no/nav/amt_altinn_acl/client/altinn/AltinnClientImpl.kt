package no.nav.amt_altinn_acl.client.altinn

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.amt_altinn_acl.utils.JsonUtils.fromJsonString
import no.nav.amt_altinn_acl.utils.SecureLog.secureLog
import no.nav.common.rest.client.RestClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory

const val pagineringSize = 500

class AltinnClientImpl(
	private val baseUrl: String,
	private val altinnApiKey: String,
	private val maskinportenTokenProvider: () -> String,
	private val client: OkHttpClient = RestClient.baseClient(),
) : AltinnClient {
	private val log = LoggerFactory.getLogger(javaClass)

	override fun hentAlleOrganisasjoner(norskIdent: String, serviceCode: String): List<String> {
		val organisasjoner = HashSet<String>()
		var ferdig = false
		var i = 0
		while (!ferdig) {
			val skip = pagineringSize * i++
			log.info("Henter organisasjoner fra Altinn, skip: $skip")
			val hentedeOrganisasjoner = hentAlleOrganisasjonerFraAltinn(norskIdent, serviceCode, skip)
			organisasjoner.addAll(hentedeOrganisasjoner)
			ferdig = hentedeOrganisasjoner.size < pagineringSize
		}
		return organisasjoner.toList()
	}

	private fun hentAlleOrganisasjonerFraAltinn(norskIdent: String, serviceCode: String, skip: Int): List<String> {
		val request = Request.Builder()
			.url("$baseUrl/api/serviceowner/reportees?subject=$norskIdent&serviceCode=$serviceCode&serviceEdition=1&\$top=$pagineringSize&\$skip=$skip")
			.addHeader("APIKEY", altinnApiKey)
			.addHeader("Authorization", "Bearer ${maskinportenTokenProvider.invoke()}")
			.get()
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				secureLog.error("Klarte ikke å hente organisasjoner for serviceCode=$serviceCode norskIdent=$norskIdent message=${response.message}, code=${response.code}, body=${response.body?.string()}")
				log.error("Klarte ikke hente organisasjoner for $serviceCode. response: ${response.code}")
				throw RuntimeException("Klarte ikke å hente organisasjoner code=${response.code}")
			}

			val body = response.body?.string()
				?: throw RuntimeException("Body is missing")

			if (!response.headers["X-Warning-LimitReached"].isNullOrEmpty()) {
				secureLog.warn("Bruker med norskIdent=$norskIdent har for mange tilganger for $serviceCode, kunne ikke hente alle")
			}

			return fromJsonString<List<ReporteeResponseEntity.Reportee>>(body)
				.filter { it.organisasjonsnummer != null }
				.mapNotNull { it.organisasjonsnummer }
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
