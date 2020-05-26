package io.olownia.domain.ethereum

case class Log(
    address: String,
    blockHash: String,
    blockNumber: String,
    data: String,
    logIndex: String,
    removed: Boolean,
    topics: Seq[String],
    transactionHash: String,
    transactionIndex: String
)
