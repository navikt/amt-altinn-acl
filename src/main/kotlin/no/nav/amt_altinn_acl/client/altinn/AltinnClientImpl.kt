package no.nav.amt_altinn_acl.client.altinn

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.amt_altinn_acl.utils.JsonUtils.fromJsonString
import no.nav.amt_altinn_acl.utils.SecureLog.secureLog
import no.nav.common.rest.client.RestClient
import okhttp3.OkHttpClient
import okhttp3.Request

class AltinnClientImpl(
	private val baseUrl: String,
	private val altinnApiKey: String,
	private val maskinportenTokenProvider: () -> String,
	private val client: OkHttpClient = RestClient.baseClient(),
) : AltinnClient {

	override fun hentOrganisasjoner(norskIdent: String, serviceCode: String?): String {
		val requestUrl = serviceCode
			?.let { "$baseUrl/api/serviceowner/reportees?subject=$norskIdent&serviceCode=$serviceCode&serviceEdition=1"}
				?: "$baseUrl/api/serviceowner/reportees?subject=$norskIdent"

		val request = Request.Builder()
			.url(requestUrl)
			.addHeader("APIKEY", altinnApiKey)
			.addHeader("Authorization", "Bearer ${maskinportenTokenProvider.invoke()}")
			.get()
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				secureLog.error("Klarte ikke å hente tilknyttede organisasjoner for norskIdent=$norskIdent message=${response.message}, code=${response.code}, body=${response.body?.string()}")
				throw RuntimeException("Klarte ikke å hente tilknyttede organisasjoner code=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")
			return body
		}
	}

	override fun hentTilknyttedeOrganisasjoner(norskIdent: String): List<Organisasjon> {
		val request = Request.Builder()
			.url("$baseUrl/api/serviceowner/reportees?subject=$norskIdent")
			.addHeader("APIKEY", altinnApiKey)
			.addHeader("Authorization", "Bearer ${maskinportenTokenProvider.invoke()}")
			.get()
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				secureLog.error("Klarte ikke å hente tilknyttede organisasjoner for norskIdent=$norskIdent message=${response.message}, code=${response.code}, body=${response.body?.string()}")
				throw RuntimeException("Klarte ikke å hente tilknyttede organisasjoner code=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val data = fromJsonString<List<HentTilknyttedeOrganisasjoner.Reportee>>(body)

			return data
				.map {
					Organisasjon(
						type = mapType(it.type) ?: return@map null,
						organisasjonsnummer = it.organizationNumber ?: return@map null
					)
				}.filterNotNull()
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

	private fun mapType(type: String): Organisasjon.Type? {
		return when (type) {
			"Enterprise" -> Organisasjon.Type.OVERORDNET_ENHET
			"Business" -> Organisasjon.Type.UNDERENHET
			else -> null
		}
	}

	object HentTilknyttedeOrganisasjoner {
		data class Reportee(
			@JsonAlias("Type")
			val type: String,

			@JsonAlias("OrganizationNumber")
			val organizationNumber: String?,
		)
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

}
