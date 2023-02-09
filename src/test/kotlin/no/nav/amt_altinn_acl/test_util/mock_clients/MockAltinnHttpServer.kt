package no.nav.amt_altinn_acl.test_util.mock_clients

import okhttp3.mockwebserver.MockResponse

class MockAltinnHttpServer : MockHttpServer(name = "Altinn Mock Server") {

	private val personJson = """
		{
			"Name": "LAGSPORT PLUTSELIG ",
			"Type": "Person",
			"SocialSecurityNumber": "11111111111"
		}
	""".trimIndent()

	fun addReporteeResponse(
		personIdent: String,
		serviceCode: String,
		organisasjonnummer: List<String>
	) {
		addResponseHandler(
			path = "/api/serviceowner/reportees?subject=$personIdent&serviceCode=$serviceCode&serviceEdition=1",
			generateReporteeResponse(organisasjonnummer)
		)
	}

	private fun generateReporteeResponse(organisasjonnummer: List<String>): MockResponse {
		val body = if (organisasjonnummer.isEmpty()) {
			"""
				[
					$personJson
				]
			""".trimIndent()
		} else {
			StringBuilder()
				.append("[")
				.append(personJson)
				.append(",")
				.append(organisasjonnummer.joinToString(",") {
					"""
					{
						"Name": "NAV NORGE AS",
						"Type": "Business",
						"OrganizationNumber": "$it",
						"ParentOrganizationNumber": "5235325325",
						"OrganizationForm": "AAFY",
						"Status": "Active"
					}
					""".trimIndent()
				})
				.append("]")
				.toString()
		}

		return MockResponse()
			.setResponseCode(200)
			.setBody(body)
	}

}
