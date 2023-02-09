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
				log.error("Klarte ikk ehente organisasjoner for $serviceCode")
				throw RuntimeException("Klarte ikke å hente organisasjoner code=${response.code}")
			}

			val body = response.body?.string() ?: return Result.failure(Exception("Body is missing"))
			val data = fromJsonString<List<ReporteeResponseEntity.Reportee>>(body)
				.filter { it.organizationNumber != null }
				.mapNotNull { it.organizationNumber }

			return Result.success(data)
		}
	}

	override fun hentRettigheter(norskIdent: String, organisasjonsnummer: String): List<AltinnRettighet> {
		val request = Request.Builder()
			.url("$baseUrl/api/serviceowner/authorization/rights?subject=$norskIdent&reportee=$organisasjonsnummer")
			.addHeader("APIKEY", altinnApiKey)
			.addHeader("Authorization", "Bearer ${maskinportenTokenProvider.invoke()}")
			.get()
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				secureLog.error("Klarte ikke å hente rettigheter for norskIdent=$norskIdent orgnr=$organisasjonsnummer")
				throw RuntimeException("Klarte ikke å hente rettigheter")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val data = fromJsonString<HentRettigheter.Response>(body)

			return data.rights
				.map { AltinnRettighet(it.serviceCode) }
		}
	}

	object HentRettigheter {
		data class Response(
			@JsonAlias("Rights")
			val rights: List<Right>,
		) {
			data class Right(
				@JsonAlias("ServiceCode")
				val serviceCode: String
			)
		}
	}

	object ReporteeResponseEntity {
		data class Reportee(
			@JsonAlias("Type")
			val type: String,

			@JsonAlias("OrganizationNumber")
			val organizationNumber: String?,
		)
	}
}
