package no.nav.amt.altinn.acl.service

import no.nav.amt.altinn.acl.client.altinn.Altinn3Client

internal fun Exception.safeStatusCode(): Int? = (this as? Altinn3Client.AltinnClientException)?.statusCode

internal fun Exception.safeErrorCode(): String? = (this as? Altinn3Client.AltinnClientException)?.errorCode
