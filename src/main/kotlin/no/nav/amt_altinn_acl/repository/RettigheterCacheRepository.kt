package no.nav.amt_altinn_acl.repository

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
				dataVersion = rs.getInt("data_version"),
				dataJson = rs.getString("data_json"),
				expiresAfter = rs.getZonedDateTime("expires_after"),
				createdAt = rs.getZonedDateTime("created_at")
			)
		}

	fun upsertData(norskIdent: String, dataVersion: Int, dataJson: String, expiresAfter: ZonedDateTime) {
		val sql = """
			INSERT INTO rettigheter_cache(norsk_ident, data_version, data_json, expires_after)
			VALUES (:norsk_ident, :data_version, cast(:data_json as json), :expires_after)
			ON CONFLICT (norsk_ident) DO UPDATE
			SET data_json = cast(:data_json as json), data_version = :data_version, expires_after = :expires_after
		""".trimIndent()

		val parameters = MapSqlParameterSource()
			.addValue("norsk_ident", norskIdent)
			.addValue("data_version", dataVersion)
			.addValue("data_json", dataJson)
			.addValue("expires_after", expiresAfter.toOffsetDateTime())

		template.update(sql, parameters)
	}

	fun hentCachetData(norskIdent: String, dataVersion: Int): RettigheterCacheDbo? {
		val sql = """
			SELECT * FROM rettigheter_cache WHERE norsk_ident = :norsk_ident AND data_version = :data_version
		""".trimIndent()

		val parameters = MapSqlParameterSource(mapOf(
			"norsk_ident" to norskIdent,
			"data_version" to dataVersion,
		))

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	fun slettCachetData(norskIdent: String) {
		val sql = """
			DELETE FROM rettigheter_cache WHERE norsk_ident = :norsk_ident
		""".trimIndent()

		val parameters = MapSqlParameterSource("norsk_ident", norskIdent)

		template.update(sql, parameters)
	}

}
