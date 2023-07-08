package com.github.windymelt.apsiren.model

// フォローを受信するのに必要な量だけ実装されている
case class Follow(
    // id: String,
    // url: String,
    // published: String,
    // to: Seq[String],
    `type`: "Follow" = "Follow",
    actor: String
)
