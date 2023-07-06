package com.github.windymelt.apsiren

case class Webfinger(subject: String, links: Seq[Webfinger.WebfingerLink])

object Webfinger {
  case class WebfingerLink(rel: String, `type`: String, href: String)
}
