package io.olownia.domain.bitcoin

// import java.time.Instant

case class Block(
    hash: String,
    size: Int,
    strippedsize: Int,
    weight: Int,
    height: Int,
    version: Int,
    // time: Instant,
    nonce: Long,
    bits: String,
    difficulty: Int,
    previousblockhash: Option[String],
    tx: Seq[Transaction]
)
