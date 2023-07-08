package com.github.windymelt.apsiren.model

case class NodeInfo(
    openRegistrations: Boolean = false,
    protocols: Seq[String] = Seq("activitypub"),
    software: NodeInfo.Software = NodeInfo.Software(),
    usage: NodeInfo.Usage = NodeInfo.Usage(),
    version: String = "2.1"
)
object NodeInfo {
  case class Software(name: String = "siren", version: String = "0.1.0")
  case class Usage(users: Users = Users())
  case class Users(total: Int = 1)
}
