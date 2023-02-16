package no.nav.amt_altinn_acl.repository

import no.nav.amt_altinn_acl.domain.RightType
import no.nav.amt_altinn_acl.repository.dbo.RightDbo
import no.nav.amt_altinn_acl.utils.DbUtils.sqlParameters
import no.nav.amt_altinn_acl.utils.getNullableZonedDateTime
import no.nav.amt_altinn_acl.utils.getZonedDateTime
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

@Repository
class RightsRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		RightDbo(
			id = rs.getLong("id"),
			personId = rs.getLong("person_id"),
			organizationNumber = rs.getString("organization_number"),
			rightType = RightType.valueOf(rs.getString("right_type")),
			validFrom = rs.getZonedDateTime("valid_from"),
			validTo = rs.getNullableZonedDateTime("valid_to")
		)
	}

	fun createRight(personId: Long, organizationNumber: String, rightType: RightType): RightDbo {
		val sql = """
			INSERT INTO rights(person_id, organization_number, right_type, valid_from)
			VALUES (:person_id, :organization_number, :right_type, current_timestamp)
		""".trimIndent()

		val params = sqlParameters(
			"person_id" to personId,
			"organization_number" to organizationNumber,
			"right_type" to rightType.toString(),
		)

		val keyHolder = GeneratedKeyHolder()
		template.update(sql, params, keyHolder)

		val id: Long = keyHolder.keys?.get("id") as Long?
			?: throw IllegalStateException("Expected key 'id' to be part of keyset")

		return get(id)
	}

	fun invalidateRight(id: Long) {
		val sql = """
			UPDATE rights
			SET valid_to = current_timestamp
			WHERE id = :id
		""".trimIndent()

		template.update(sql, sqlParameters("id" to id))
	}

	fun getRightsForPerson(personId: Long, onlyValid: Boolean = true): List<RightDbo> {
		return if (onlyValid) getValidRightsForPerson(personId)
		else getAllRightsForPerson(personId)
	}

	private fun getValidRightsForPerson(personId: Long): List<RightDbo> {
		val sql = """
			SELECT * from rights
			WHERE person_id = :person_id
			AND valid_to is null
		""".trimIndent()

		return template.query(sql, sqlParameters("person_id" to personId), rowMapper)
	}

	private fun getAllRightsForPerson(personId: Long): List<RightDbo> {
		val sql = """
			SELECT * from rights
			WHERE person_id = :person_id
		""".trimIndent()

		return template.query(sql, sqlParameters("person_id" to personId), rowMapper)
	}

	private fun get(id: Long): RightDbo {
		return template.query(
			"SELECT * FROM rights WHERE id = :id",
			sqlParameters("id" to id),
			rowMapper
		).first()
	}

}
