package no.nav.amt_altinn_acl.repository

import no.nav.amt_altinn_acl.domain.RoleType
import no.nav.amt_altinn_acl.repository.dbo.RoleDbo
import no.nav.amt_altinn_acl.utils.DbUtils.sqlParameters
import no.nav.amt_altinn_acl.utils.getNullableZonedDateTime
import no.nav.amt_altinn_acl.utils.getZonedDateTime
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

@Repository
class RoleRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		RoleDbo(
			id = rs.getLong("id"),
			personId = rs.getLong("person_id"),
			organizationNumber = rs.getString("organization_number"),
			roleType = RoleType.valueOf(rs.getString("role")),
			validFrom = rs.getZonedDateTime("valid_from"),
			validTo = rs.getNullableZonedDateTime("valid_to")
		)
	}

	fun createRole(personId: Long, organizationNumber: String, roleType: RoleType): RoleDbo {
		val sql = """
			INSERT INTO role(person_id, organization_number, role, valid_from)
			VALUES (:person_id, :organization_number, :role, current_timestamp)
		""".trimIndent()

		val params = sqlParameters(
			"person_id" to personId,
			"organization_number" to organizationNumber,
			"role" to roleType.toString(),
		)

		val keyHolder = GeneratedKeyHolder()
		template.update(sql, params, keyHolder)

		val id: Long = keyHolder.keys?.get("id") as Long?
			?: throw IllegalStateException("Expected key 'id' to be part of keyset")

		return get(id)
	}

	fun invalidateRole(id: Long) {
		val sql = """
			UPDATE role
			SET valid_to = current_timestamp
			WHERE id = :id
		""".trimIndent()

		template.update(sql, sqlParameters("id" to id))
	}

	fun getRolesForPerson(personId: Long, onlyValid: Boolean = true): List<RoleDbo> {
		return if (onlyValid) getValidRolesForPerson(personId)
		else getAllRolesForPerson(personId)
	}

	private fun getValidRolesForPerson(personId: Long): List<RoleDbo> {
		val sql = """
			SELECT * from role
			WHERE person_id = :person_id
			AND valid_to is null
		""".trimIndent()

		return template.query(sql, sqlParameters("person_id" to personId), rowMapper)
	}

	private fun getAllRolesForPerson(personId: Long): List<RoleDbo> {
		val sql = """
			SELECT * from role
			WHERE person_id = :person_id
		""".trimIndent()

		return template.query(sql, sqlParameters("person_id" to personId), rowMapper)
	}

	private fun get(id: Long): RoleDbo {
		return template.query(
			"SELECT * FROM role WHERE id = :id",
			sqlParameters("id" to id),
			rowMapper
		).first()
	}

}
