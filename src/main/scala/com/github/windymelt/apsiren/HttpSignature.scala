package com.github.windymelt.apsiren

import akka.actor.typed.ActorSystem
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
import java.security.MessageDigest
import java.security.PrivateKey
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.io.Source

object HttpSignature {
  val signAlgo = SigningAlgorithm.RSA_SHA256
  val algo = Algorithm.RSA_SHA256
  val keyId = "https://siren.capslock.dev/actor"
  val signature: Signature = new Signature(
    keyId,
    signAlgo,
    algo,
    null,
    null,
    java.util.List
      .of(
        "(request-target)",
        "host",
        "date" // , "content-type"
      )
  )
  val postSignature: Signature =
    new Signature(
      keyId,
      signAlgo,
      algo,
      null,
      null,
      java.util.List
        .of(
          "(request-target)",
          "host",
          "date",
          "digest"
          // "content-type"
        )
    );
  val keyIS = new StringBufferInputStream(
    Source.fromFile("server.key").getLines().mkString("\n")
  )
  val key = PEM.readPrivateKey(keyIS)
  def sign(
      req: HttpRequest
  )(implicit ctx: ActorSystem[Nothing]): HttpRequest = {
    var mutableReq = req
    val map = new java.util.HashMap[String, String]()

    val signer: Signer = new Signer(
      key,
      req.method match {
        case akka.http.scaladsl.model.HttpMethods.POST => postSignature
        case _                                         => signature
      }
    )
    if (req.method == akka.http.scaladsl.model.HttpMethods.POST) {
      val md = MessageDigest.getInstance("SHA-256")
      val entity = Await.result(
        req.entity.toStrict(FiniteDuration(3, "seconds")),
        FiniteDuration(3, "seconds")
      )
      val digest = md.digest(entity.data.toArrayUnsafe())
      val b64enc = java.util.Base64.getEncoder()
      val b64Str = "sha-256=" + b64enc.encodeToString(digest)
      // Digestはdeprecatedらしいがどのインスタンスもこれを利用している・・・。
      val akka.http.scaladsl.model.HttpHeader.ParsingResult
        .Ok(digestHeader, _) =
        akka.http.scaladsl.model.HttpHeader.parse("Digest", b64Str)
      mutableReq = mutableReq.withHeaders(
        mutableReq.headers ++ Seq(digestHeader)
      )
    }
    for (h <- mutableReq.headers) {
      map.put(h.name(), h.value())
    }
    // map.put("Content-Type", mutableReq.entity.contentType.value)
    map.put("Host", mutableReq.uri.authority.host.address()) // TODO: fill port
    // uriと書いてあるが嘘で、path+paramでよい
    val pathAndQuery = mutableReq.uri.path
      .toString() ++ mutableReq.uri.rawQueryString.getOrElse("")
    val signed =
      signer.sign(mutableReq.method.value, pathAndQuery, map)
    val HttpHeader.ParsingResult.Ok(header, _) =
      HttpHeader.parse(
        "Signature",
        signed.toParamString()
      )
    mutableReq = mutableReq.withHeaders(header, mutableReq.headers: _*)

    mutableReq
  }
}
