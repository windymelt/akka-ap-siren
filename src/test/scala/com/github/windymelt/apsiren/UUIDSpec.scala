package com.github.windymelt.apsiren

import org.scalacheck.Gen
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class UUIDSpec
    extends AnyFunSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  val uuidGen = Gen.delay(Gen.const(UUID.generate()))

  describe("UUID") {
    it("can generate UUID") {
      val id = UUID.generate().base64Stripped
      note(id)
      id.length shouldBe 22
    }

    it("can convert into Base64 representation and back") {
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

    it("can convert into BigInt representation and back") {
      forAll(uuidGen) { uuid =>
        val bigIntRepresentation = uuid.bigIntRepresentation
        note(bigIntRepresentation.toString)
        UUID.fromBigIntRepresentation(bigIntRepresentation) shouldBe Some(uuid)
      }
    }

    describe("BigInt representation") {
      it("should be always positive") {
        forAll(uuidGen) { uuid =>
          (uuid.bigIntRepresentation > 0) shouldBe true
        }
      }
    }
  }
}
