package com.github.windymelt.apsiren.model

case class Webfinger(subject: String, links: Seq[Webfinger.WebfingerLink])

object Webfinger {
  case class WebfingerLink(rel: String, `type`: String, href: String)
}
