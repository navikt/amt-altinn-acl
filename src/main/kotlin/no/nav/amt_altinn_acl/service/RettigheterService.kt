package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.domain.Bruker
import no.nav.amt_altinn_acl.repository.RettigheterCacheRepository
import no.nav.amt_altinn_acl.repository.dbo.RettigheterCacheDbo
import no.nav.amt_altinn_acl.utils.JsonUtils
import no.nav.amt_altinn_acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
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

	private val log = LoggerFactory.getLogger(javaClass)

	private val relevanteServiceKoder = listOf(altinnKoordinatorServiceCode, altinnVeilederServiceCode)

	companion object {
		const val CACHE_VERSION = 2
		const val CACHE_EXPIRATION_HOURS = 15L
	}

	fun hentUtdaterteBrukere(batchSize: Int = 25): List<Bruker> {
		return rettigheterCacheRepository
			.hentUtdaterteBrukere(batchSize)
			.map { it.toModel() }
	}

	fun synkroniserBrukere(personligeIdenter: List<String>) {
		log.info("Starter synkronisering av ${personligeIdenter.size} brukere med utgått tilgang")
		secureLog.info("Starter synkronisering av ${personligeIdenter.size} brukere med utgått tilgang")

		personligeIdenter.forEach { personligIdent ->
			secureLog.info("Starter synkronisering av rettigheter for $personligIdent")
			val rettigheter = hentAlleRettigheter(personligIdent)
			secureLog.info("Synkroniserte rettigheter for $personligIdent, bruker har nå ${rettigheter.size} rettigheter")
		}
		log.info("Fullført synkronisering av ${personligeIdenter.size} brukere med utgått tilgang")
		secureLog.info("Fullført synkronisering av ${personligeIdenter.size} brukere med utgått tilgang")

	}

	fun hentAlleRettigheter(norskIdent: String): List<AltinnRettighet> {
		secureLog.info("Henter alle rettigheter for $norskIdent")

		val cachetRettigheterDbo = rettigheterCacheRepository.hentCachetData(norskIdent, CACHE_VERSION)
		val hasExpired = cachetRettigheterDbo == null || ZonedDateTime.now().isAfter(cachetRettigheterDbo.expiresAfter)
		val cachetRettigheter = cachetRettigheterDbo?.let { JsonUtils.fromJsonString<CachetRettigheter>(it.dataJson) }

		if (!hasExpired && cachetRettigheter != null && cachetRettigheter.rettigheter.isNotEmpty()) {
			return cachetRettigheter.rettigheter
		}

		secureLog.info("Starter uthenting av altinn rettigheter for $norskIdent")

		try {
			val rettigheter = hentRettigheterFraAltinn(norskIdent)
			secureLog.info("Fullført uthenting av altinn rettigheter for $norskIdent. Personen har nå ${rettigheter.size} rettigheter")
			oppdaterRettigheterCache(norskIdent, rettigheter)
			return rettigheter
		} catch (t: Throwable) {
			log.error("Klarte ikke å hente rettigheter", t)
			secureLog.error("Klarte ikke å hente rettigheter for ident $norskIdent", t)

			if (cachetRettigheter != null) {
				secureLog.warn("Bruker utdaterte rettigheter for ident $norskIdent")
				return cachetRettigheter.rettigheter
			}

			throw RuntimeException("Klarte ikke å hente Altinn rettigheter")
		}
	}

	private fun hentRettigheterFraAltinn(norskIdent: String): List<AltinnRettighet> {
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
			.filter { relevanteServiceKoder.contains(it.serviceCode) }
			.toList()
	}

	private fun oppdaterRettigheterCache(norskIdent: String, rettigheter: List<AltinnRettighet>) {
		val json = JsonUtils.toJsonString(CachetRettigheter(rettigheter = rettigheter))
		val expiration = ZonedDateTime.now().plusHours(CACHE_EXPIRATION_HOURS)

		rettigheterCacheRepository.upsertData(norskIdent, CACHE_VERSION, json, expiration)
	}

	data class CachetRettigheter(
		val rettigheter: List<AltinnRettighet>,
	)

	private fun RettigheterCacheDbo.toModel() = Bruker(
		personligIdent = norskIdent,
		expiresAfter = expiresAfter,
		tilganger = dataJson.let { JsonUtils.fromJsonString<CachetRettigheter>(it) }.rettigheter
	)
}
