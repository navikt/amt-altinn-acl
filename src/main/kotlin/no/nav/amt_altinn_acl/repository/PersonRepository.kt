package no.nav.amt_altinn_acl.repository

import no.nav.amt_altinn_acl.repository.dbo.PersonDbo
import no.nav.amt_altinn_acl.utils.DbUtils.sqlParameters
import no.nav.amt_altinn_acl.utils.getZonedDateTime
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Repository
class PersonRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		PersonDbo(
			id = rs.getLong("id"),
			norskIdent = rs.getString("norsk_ident"),
			created = rs.getZonedDateTime("created"),
			lastSynchronized = rs.getZonedDateTime("last_synchronized")
		)
	}

	fun getOrCreate(norskIdent: String): PersonDbo {
		return get(norskIdent)
			?: create(norskIdent)
	}

	fun setSynchronized(norskIdent: String, lastSynchronized: ZonedDateTime = ZonedDateTime.now()) {
		val sql = """
			UPDATE person
			SET last_synchronized = :last_synchronized
			WHERE norsk_ident = :norsk_ident
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"norsk_ident" to norskIdent,
				"last_synchronized" to LocalDateTime.from(lastSynchronized)
			)
		)
	}

	fun getUnsynchronizedPersons(maxSize: Int, synchronizedBefore: LocalDateTime): List<PersonDbo> {
		val sql = """
			SELECT *
			FROM person
			WHERE last_synchronized < :synchronized_before
			ORDER BY last_synchronized asc
			limit :limit
		""".trimIndent()

		val parameters = sqlParameters(
			"limit" to maxSize,
			"synchronized_before" to synchronizedBefore
		)

		return template.query(sql, parameters, rowMapper)
	}

	private fun get(norskIdent: String): PersonDbo? {
		return template.query(
			"SELECT * FROM person WHERE norsk_ident = :norsk_ident",
			sqlParameters("norsk_ident" to norskIdent),
			rowMapper
		).firstOrNull()
	}

	private fun create(norskIdent: String): PersonDbo {
		val sql = """
			INSERT INTO person(norsk_ident)
			VALUES (:norsk_ident)
		""".trimIndent()

		val params = sqlParameters("norsk_ident" to norskIdent)

		template.update(sql, params)

		return get(norskIdent) ?: throw NoSuchElementException("Person ikke funnet")
	}

}
