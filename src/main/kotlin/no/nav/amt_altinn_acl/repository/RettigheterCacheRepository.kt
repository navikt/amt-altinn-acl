package no.nav.amt_altinn_acl.repository

import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.repository.dbo.RettigheterCacheDbo
import no.nav.amt_altinn_acl.utils.getZonedDateTime
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class RettigheterCacheRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper =
		RowMapper { rs, _ ->
			RettigheterCacheDbo(
				id = rs.getLong("id"),
				norskIdent = rs.getString("norsk_ident"),
				rettigheterJson = rs.getString("rettigheter_json"),
				expiresAfter = rs.getZonedDateTime("expires_after"),
				createdAt = rs.getZonedDateTime("created_at")
			)
		}

	fun upsertRettigheter(norskIdent: String, rettigheterJson: String, expiresAfter: ZonedDateTime) {
		val sql = """
			INSERT INTO rettigheter_cache(norsk_ident, rettigheter_json, expires_after)
			VALUES (:norsk_ident, cast(:rettigheter_json as json), :expires_after)
			ON CONFLICT (norsk_ident) DO UPDATE
			SET rettigheter_json = cast(:rettigheter_json as json), expires_after = :expires_after
		""".trimIndent()

		val parameters = MapSqlParameterSource()
			.addValue("norsk_ident", norskIdent)
			.addValue("rettigheter_json", rettigheterJson)
			.addValue("expires_after", expiresAfter.toOffsetDateTime())

		template.update(sql, parameters)
	}

	fun hentRettigheter(norskIdent: String): RettigheterCacheDbo? {
		val sql = """
			SELECT * FROM rettigheter_cache WHERE norsk_ident = :norsk_ident
		""".trimIndent()

		val parameters = MapSqlParameterSource("norsk_ident", norskIdent)

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	fun slettRettigheter(norskIdent: String) {
		val sql = """
			DELETE FROM rettigheter_cache WHERE norsk_ident = :norsk_ident
		""".trimIndent()

		val parameters = MapSqlParameterSource("norsk_ident", norskIdent)

		template.update(sql, parameters)
	}

}
