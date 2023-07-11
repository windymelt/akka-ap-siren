package com.github.windymelt.apsiren

import org.scalacheck.Gen
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class UUIDSpec
    extends AnyFunSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  describe("UUID") {
    it("can generate UUID") {
      val id = UUID.generate().base64Stripped
      note(id)
      id.length shouldBe 22
    }

    it("can convert into Base64 representation and back") {
      val uuidGen = Gen.delay(Gen.const(UUID.generate()))

      forAll(uuidGen) { uuid =>
        val b64Representation = uuid.base64Stripped
        note(b64Representation)
        UUID.fromBase64Stripped(b64Representation) shouldBe Some(uuid)
      }
    }

    it("can deny broken representation") {
      val broken = "foobar123456@aaaaaaaaa"
      UUID.fromBase64Stripped(broken) shouldBe None
    }
  }
}
