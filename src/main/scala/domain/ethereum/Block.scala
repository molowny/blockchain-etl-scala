package io.olownia.domain.ethereum

case class Block(
    hash: String,
    number: String,
    transactions: Seq[Transaction]
)
