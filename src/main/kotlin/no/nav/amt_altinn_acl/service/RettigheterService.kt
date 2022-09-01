package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.repository.RettigheterCacheRepository
import no.nav.amt_altinn_acl.utils.JsonUtils
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class RettigheterService(
	private val altinnClient: AltinnClient,
	private val rettigheterCacheRepository: RettigheterCacheRepository
) {

	companion object {
		const val CACHE_EXPIRATION_HOURS = 12L
	}

	fun hentAlleRettigheter(norskIdent: String): List<AltinnRettighet> {
		val cachetRettigheter = hentAlleCachedeRettigheter(norskIdent)

		if (cachetRettigheter != null) {
			return cachetRettigheter
		}

		val rettigheter = hentAlleRettigheterFraAltinn(norskIdent)

		cacheRettigheter(norskIdent, rettigheter)

		return rettigheter
	}

	private fun hentAlleRettigheterFraAltinn(norskIdent: String): List<AltinnRettighet> {
		val virksomheter = altinnClient.hentTilknyttedeOrganisasjoner(norskIdent)

		return virksomheter.parallelStream()
			.flatMap {
				altinnClient.hentRettigheter(norskIdent, it.organisasjonsnummer)
					.map { r -> AltinnRettighet(
						organisasjonsnummmer = it.organisasjonsnummer,
						rettighetId = r.rettighetId.toString()
					) }
					.stream()
			}
			.toList()
	}

	private fun cacheRettigheter(norskIdent: String, rettigheter: List<AltinnRettighet>) {
		val json = JsonUtils.toJsonString(CachetRettigheter(rettigheter = rettigheter))
		val expiration = ZonedDateTime.now().plusHours(CACHE_EXPIRATION_HOURS)

		rettigheterCacheRepository.upsertRettigheter(norskIdent, json, expiration)
	}

	private fun hentAlleCachedeRettigheter(norskIdent: String): List<AltinnRettighet>? {
		val cachetRettigheterDbo = rettigheterCacheRepository.hentRettigheter(norskIdent) ?: return null

		val hasExpired = ZonedDateTime.now().isAfter(cachetRettigheterDbo.expiresAfter)

		if (hasExpired) {
			rettigheterCacheRepository.slettRettigheter(norskIdent)
			return null
		}

		val cachetRettigheter = JsonUtils.fromJsonString<CachetRettigheter>(cachetRettigheterDbo.rettigheterJson)

		return cachetRettigheter.rettigheter
	}

	data class CachetRettigheter(
		val version: Int = 1,
		val rettigheter: List<AltinnRettighet>,
	)

}
