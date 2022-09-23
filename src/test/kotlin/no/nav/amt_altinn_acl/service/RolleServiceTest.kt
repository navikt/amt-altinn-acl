package no.nav.amt_altinn_acl.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.domain.TiltaksarrangorRolleType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RolleServiceTest {

	val koordinatorServiceKode = "1234"

	val veilederServiceKode = "5678"

	lateinit var rettigheterService: RettigheterService

	lateinit var rolleService: RolleService

	@BeforeEach
	fun setup() {
		rettigheterService = mockk()
		rolleService = RolleService(
			altinnKoordinatorServiceCode = koordinatorServiceKode,
			altinnVeilederServiceCode = veilederServiceKode,
			rettigheterService
		)
	}

	@Test
	fun `hentTiltaksarrangorRoller - skal hente tiltaksarrangor roller`(){
		val norskIdent = "12354"
		val organisasjonsnummer = "53928442"

		every {
			rettigheterService.hentAlleRettigheter(norskIdent)
		} returns listOf(
			AltinnRettighet(organisasjonsnummer, koordinatorServiceKode),
			AltinnRettighet("42378943", "374892")
		)

		val roller = rolleService.hentTiltaksarrangorRoller(norskIdent)

		roller shouldHaveSize 1

		roller.first().organisasjonsnummer shouldBe organisasjonsnummer
		roller.first().roller shouldBe listOf(TiltaksarrangorRolleType.KOORDINATOR)
	}

}
