package io.olownia.domain.bitcoin

case class Transaction(
    hash: String,
    size: Int,
    vsize: Int,
    weight: Int,
    version: Int,
    blockhash: Option[String]
    // time: Instant
)
