package no.nav.amt_altinn_acl.utils

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource

object DbUtils {

	fun <V> sqlParameters(vararg pairs: Pair<String, V>): MapSqlParameterSource {
		return MapSqlParameterSource().addValues(pairs.toMap())
	}

}
