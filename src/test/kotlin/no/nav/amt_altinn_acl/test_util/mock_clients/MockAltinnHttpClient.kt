package no.nav.amt_altinn_acl.test_util.mock_clients

import no.nav.amt_altinn_acl.test_util.MockHttpClient
import kotlin.random.Random

class MockAltinnHttpClient : MockHttpClient() {

	fun enqueueHentOrganisasjonerResponse(organisasjonnummer: List<String>) {
		if (organisasjonnummer.isEmpty()) {
			throw IllegalArgumentException("Trenger minst 1 organisasjonsnummer")
		}

		val organisasjonerJson = organisasjonnummer.joinToString(",") {
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
		}

		dispatch(
			body = """
					[
						{
							"Name": "LAGSPORT PLUTSELIG ",
							"Type": "Person",
							"SocialSecurityNumber": "11111111111"
						},
						$organisasjonerJson
					]
				"""
		)
	}
}
