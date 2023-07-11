package com.github.windymelt.apsiren

import java.nio.ByteBuffer
import java.util.Base64
import scala.collection.BitSet

case class UUID(private val inner: Array[Byte]) {
  def base64Stripped: String = base64.take(22)
  private def base64: String = {
    val enc = Base64.getUrlEncoder()
    enc.encodeToString(inner)
  }
  override def equals(sth: Any): Boolean = sth match {
    case UUID(arr) => inner.toSeq == arr.toSeq
    case _         => false
  }
  override def toString(): String = s"UUID(${base64Stripped})"
}

object UUID {
  def generate(): UUID = {
    // 今のところUUIDv4にしておくがそのうち良いやつにする
    val uuid = java.util.UUID.randomUUID()

    val buffer = Array.ofDim[Byte](16)
    val bbuf = ByteBuffer.allocate(8) // 64 bits(long)

    // copy upper
    bbuf.putLong(uuid.getMostSignificantBits())
    bbuf.array().copyToArray(buffer, 0)
    // copy lower
    bbuf.clear()
    bbuf.putLong(uuid.getLeastSignificantBits())
    bbuf.array().copyToArray(buffer, 8)

    UUID(buffer)
  }

  def fromBase64Stripped(base64Stripped: String): Option[UUID] = {
    import scala.util.control.Exception.allCatch

    allCatch opt {
      val dec = Base64.getUrlDecoder()
      val padded = (base64Stripped ++ "==").take(24)
      UUID(dec.decode(padded))
    }
  }
}
