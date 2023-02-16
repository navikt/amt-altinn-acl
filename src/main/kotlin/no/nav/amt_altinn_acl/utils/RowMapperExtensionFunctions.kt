package no.nav.amt_altinn_acl.utils

import java.sql.ResultSet
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

fun ResultSet.getNullableZonedDateTime(columnLabel: String): ZonedDateTime? {
	val timestamp = this.getTimestamp(columnLabel) ?: return null
	return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp.time), ZoneOffset.systemDefault())
}

fun ResultSet.getZonedDateTime(columnLabel: String): ZonedDateTime {
	return getNullableZonedDateTime(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")
}

fun ResultSet.getUUID(columnLabel: String): UUID {
	return getNullableUUID(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")
}

fun ResultSet.getNullableUUID(columnLabel: String): UUID? {
	return this.getString(columnLabel)
		?.let { UUID.fromString(it) }
}
