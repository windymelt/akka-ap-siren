package com.github.windymelt.apsiren

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpRequest
import org.tomitribe.auth.signatures.Algorithm
import org.tomitribe.auth.signatures.PEM
import org.tomitribe.auth.signatures.Signature
import org.tomitribe.auth.signatures.Signer
import org.tomitribe.auth.signatures.SigningAlgorithm

import java.io.InputStream
import java.io.StringBufferInputStream
import java.security.Key
import java.security.PrivateKey
import javax.crypto.spec.SecretKeySpec
import scala.io.Source

object HttpSignature {
  val signAlgo = SigningAlgorithm.RSA_SHA256
  val algo = Algorithm.RSA_SHA256
  val signature: Signature =
    new Signature(
      "key-alias",
      signAlgo.getAlgorithmName(),
      algo.toString(),
      null,
      java.util.List.of()
    ); // (1)
  val keyIS = new StringBufferInputStream(
    Source.fromFile("server.key").getLines().mkString("\n")
  )
  val key = PEM.readPrivateKey(keyIS)
  val signer: Signer = new Signer(key, signature); // (3)
  def sign(req: HttpRequest): HttpRequest = {
    val map = new java.util.HashMap[String, String]()
    for (h <- req.headers) {
      map.put(h.name(), h.value())
    }
    val signed = signer.sign(req.method.value, req.uri.toString(), map)
    val HttpHeader.ParsingResult.Ok(header, _) =
      HttpHeader.parse(
        "Signature",
        signed.toString().drop(10 /* drop "Signature " */ )
      )
    req.addHeader(header)
  }
}
