package io.olownia

import scala.concurrent.duration.Duration

case class PostgresConfig(
    driver: String,
    url: String,
    username: String,
    password: String
)

case class RedisConfig(
    url: String
)

case class RetryPolicyConfig(
    attempts: Int,
    delay: Duration
)

case class BitcoinConfig(
    node: String,
    username: String,
    password: String,
    fromBlock: Int,
    toBlock: Int,
    chunkSize: Int,
    maxConcurrent: Int,
    retryPolicy: RetryPolicyConfig
)

case class EthereumConfig(
    node: String,
    username: Option[String],
    password: Option[String],
    fromBlock: Int,
    toBlock: Int,
    chunkSize: Int,
    maxConcurrent: Int,
    retryPolicy: RetryPolicyConfig
)

case class Config(
    httpThreads: Int,
    dbThreads: Int,
    postgres: PostgresConfig,
    redis: RedisConfig,
    bitcoin: BitcoinConfig,
    ethereum: EthereumConfig
)
