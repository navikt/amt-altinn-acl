package no.nav.amt_altinn_acl.test_util.mock_clients

import no.nav.amt_altinn_acl.client.altinn.Altinn3ClientImpl
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockAltinnHttpServer : MockHttpServer(name = "Altinn Mock Server") {
	fun addAuthorizedPartiesResponse(
		personIdent: String,
		roller: List<RolleType>,
		organisasjonnummer: List<String>,
	) {
		val authorizedPartiesRequest = Altinn3ClientImpl.AuthorizedPartiesRequest(personIdent)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/accessmanagement/api/v1/resourceowner/authorizedparties"
				&& req.method == "POST"
				&& req.getBodyAsString() == JsonUtils.objectMapper.writeValueAsString(authorizedPartiesRequest)
		}

		addResponseHandler(
			predicate = requestPredicate,
			generateAuthorizedpartiesResponse(organisasjonnummer, roller)
		)
	}

	fun addFailureResponse(
		responseCode: Int,
	) {
		addResponseHandler(
			path = "$/accessmanagement/api/v1/resourceowner/authorizedparties",
			response = MockResponse().setResponseCode(responseCode)
		)
	}

	private fun generateAuthorizedpartiesResponse(
		organisasjonnummer: List<String>,
		roller: List<RolleType>
	): MockResponse {
		val parties = organisasjonnummer.map {
			Altinn3ClientImpl.AuthorizedParty(
				organizationNumber = it,
				authorizedResources = roller.map { rolleType -> rolleType.resourceId }.toSet(),
				emptyList()
			)
		}

		return MockResponse()
			.setResponseCode(200)
			.setBody(JsonUtils.objectMapper.writeValueAsString(parties))
	}
}

