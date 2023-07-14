package com.github.windymelt.apsiren.model

case class NodeInfo(
    openRegistrations: Boolean = false,
    protocols: Seq[String] = Seq("activitypub"),
    software: NodeInfo.Software = NodeInfo.Software(),
    usage: NodeInfo.Usage = NodeInfo.Usage(),
    version: String = "2.1"
)
object NodeInfo {
  case class Software(
      name: String = "Knaviator",
      version: String = "0.1.0",
      metadata: MetaData = MetaData()
  )
  case class Usage(users: Users = Users())
  case class Users(total: Int = 1)

  // TODO: application.conf
  case class MetaData(
      nodeName: String = "siren.capslock.dev[Knaviator]",
      nodeDescription: String = "田舎の昼のサイレンbotを運用しているサーバです。"
  )
}

case class NodeInfoTop(links: Seq[NodeInfoTop.Link])
object NodeInfoTop {
  case class Link(rel: String, href: String)
}
