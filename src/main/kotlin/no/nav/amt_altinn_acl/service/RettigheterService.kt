package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.repository.RettigheterCacheRepository
import no.nav.amt_altinn_acl.utils.JsonUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class RettigheterService(
	@Value("\${altinn.koordinator-service-code}") altinnKoordinatorServiceCode: String,
	@Value("\${altinn.veileder-service-code}") altinnVeilederServiceCode: String,
	private val altinnClient: AltinnClient,
	private val rettigheterCacheRepository: RettigheterCacheRepository
) {

	private val relevanteServiceKoder = listOf(altinnKoordinatorServiceCode, altinnVeilederServiceCode)

	companion object {
		const val CACHE_VERSION = 2
		const val CACHE_EXPIRATION_HOURS = 12L
	}

	fun hentAlleRettigheter(norskIdent: String): List<AltinnRettighet> {
		val cachetRettigheter = hentAlleCachedeRettigheter(norskIdent)

		if (cachetRettigheter != null) {
			return cachetRettigheter
		}

		val rettigheter = hentAlleRettigheterFraAltinn(norskIdent)
			.filter { relevanteServiceKoder.contains(it.serviceCode) }

		cacheRettigheter(norskIdent, rettigheter)

		return rettigheter
	}

	private fun hentAlleRettigheterFraAltinn(norskIdent: String): List<AltinnRettighet> {
		val virksomheter = altinnClient.hentTilknyttedeOrganisasjoner(norskIdent)

		return virksomheter.parallelStream()
			.flatMap {
				altinnClient.hentRettigheter(norskIdent, it.organisasjonsnummer)
					.map { r -> AltinnRettighet(
						organisasjonsnummer = it.organisasjonsnummer,
						serviceCode = r.serviceCode
					) }
					.stream()
			}
			.toList()
	}

	private fun cacheRettigheter(norskIdent: String, rettigheter: List<AltinnRettighet>) {
		val json = JsonUtils.toJsonString(CachetRettigheter(rettigheter = rettigheter))
		val expiration = ZonedDateTime.now().plusHours(CACHE_EXPIRATION_HOURS)

		rettigheterCacheRepository.upsertData(norskIdent, CACHE_VERSION, json, expiration)
	}

	private fun hentAlleCachedeRettigheter(norskIdent: String): List<AltinnRettighet>? {
		val cachetRettigheterDbo = rettigheterCacheRepository.hentCachetData(norskIdent, CACHE_VERSION) ?: return null

		val hasExpired = ZonedDateTime.now().isAfter(cachetRettigheterDbo.expiresAfter)

		if (hasExpired) {
			rettigheterCacheRepository.slettCachetData(norskIdent)
			return null
		}

		val cachetRettigheter = JsonUtils.fromJsonString<CachetRettigheter>(cachetRettigheterDbo.dataJson)

		return cachetRettigheter.rettigheter
	}

	data class CachetRettigheter(
		val rettigheter: List<AltinnRettighet>,
	)

}
