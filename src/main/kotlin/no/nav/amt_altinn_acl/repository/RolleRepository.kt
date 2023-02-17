package no.nav.amt_altinn_acl.repository

import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.repository.dbo.RolleDbo
import no.nav.amt_altinn_acl.utils.DbUtils.sqlParameters
import no.nav.amt_altinn_acl.utils.getNullableZonedDateTime
import no.nav.amt_altinn_acl.utils.getZonedDateTime
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

@Repository
class RolleRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		RolleDbo(
			id = rs.getLong("id"),
			personId = rs.getLong("person_id"),
			organizationNumber = rs.getString("organization_number"),
			rolleType = RolleType.valueOf(rs.getString("rolle")),
			validFrom = rs.getZonedDateTime("valid_from"),
			validTo = rs.getNullableZonedDateTime("valid_to")
		)
	}

	fun createRolle(personId: Long, organizationNumber: String, rolleType: RolleType): RolleDbo {
		val sql = """
			INSERT INTO rolle(person_id, organization_number, rolle, valid_from)
			VALUES (:person_id, :organization_number, :rolle, current_timestamp)
		""".trimIndent()

		val params = sqlParameters(
			"person_id" to personId,
			"organization_number" to organizationNumber,
			"rolle" to rolleType.toString(),
		)

		val keyHolder = GeneratedKeyHolder()
		template.update(sql, params, keyHolder)

		val id: Long = keyHolder.keys?.get("id") as Long?
			?: throw IllegalStateException("Expected key 'id' to be part of keyset")

		return get(id)
	}

	fun invalidateRolle(id: Long) {
		val sql = """
			UPDATE rolle
			SET valid_to = current_timestamp
			WHERE id = :id
		""".trimIndent()

		template.update(sql, sqlParameters("id" to id))
	}

	fun getRollerForPerson(personId: Long, onlyValid: Boolean = true): List<RolleDbo> {
		return if (onlyValid) getValidRollerForPerson(personId)
		else getAllRollerForPerson(personId)
	}

	private fun getValidRollerForPerson(personId: Long): List<RolleDbo> {
		val sql = """
			SELECT * from rolle
			WHERE person_id = :person_id
			AND valid_to is null
		""".trimIndent()

		return template.query(sql, sqlParameters("person_id" to personId), rowMapper)
	}

	private fun getAllRollerForPerson(personId: Long): List<RolleDbo> {
		val sql = """
			SELECT * from rolle
			WHERE person_id = :person_id
		""".trimIndent()

		return template.query(sql, sqlParameters("person_id" to personId), rowMapper)
	}

	private fun get(id: Long): RolleDbo {
		return template.query(
			"SELECT * FROM rolle WHERE id = :id",
			sqlParameters("id" to id),
			rowMapper
		).first()
	}

}
