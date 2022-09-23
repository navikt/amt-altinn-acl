package no.nav.amt_altinn_acl.test_util.mock_clients

import no.nav.amt_altinn_acl.test_util.MockHttpClient
import kotlin.random.Random

class MockAltinnHttpClient : MockHttpClient() {

	fun enqueueHentTilknyttedeEnheterResponse(organisasjonnummere: List<String>) {
		if (organisasjonnummere.isEmpty()) {
			throw IllegalArgumentException("Trenger minst 1 organisasjonsnummer")
		}

		val organisasjonerJson = organisasjonnummere.joinToString(",") {
			"""
				{
					"Name": "NONFIGURATIV KOMFORTABEL HUND DA",
					"Type": "Business",
					"OrganizationNumber": "$it",
					"ParentOrganizationNumber": "999987004",
					"OrganizationForm": "BEDR",
					"Status": "Active"
				}
			""".trimIndent()
		}

		enqueue(
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

	fun enqueueHentRettigheterResponse(serviceCodes: List<String>) {
		val rettigheterJson = serviceCodes.joinToString(",") {
			"""
				{
					"ServiceCode": "$it",
					"Action": "Read",
					"RightID": ${Random.nextInt()},
					"RightType": "Service",
					"ServiceEditionCode": 1,
					"RightSourceType": "RoleTypeRights",
					"IsDelegatable": true
				}
			""".trimIndent()
		}

		enqueue(
			body = """
					{
						"Subject": {
							"Name": "LAGSPORT PLUTSELIG ",
							"Type": "Person",
							"SocialSecurityNumber": "99999098174"
						},
						"Reportee": {
							"Name": "NONFIGURATIV KOMFORTABEL HUND DA",
							"Type": "Business",
							"OrganizationNumber": "999919596",
							"OrganizationForm": "BEDR",
							"Status": "Active"
						},
						"Rights": [
							$rettigheterJson
						]
					}
				""".trimIndent()
		)
	}

}
