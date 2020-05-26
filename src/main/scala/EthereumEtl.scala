package io.olownia

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.LazyLogging
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout._
import doobie.util.transactor.Transactor
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{Logger, Retry, RetryPolicy}
import pureconfig._
import pureconfig.generic.auto._

import io.olownia.rpc.EthereumClient
import io.olownia.streams._
import io.olownia.streams.eth._

object EthereumEtl extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    val app = for {
      config <- Resource.liftF(IO.delay(ConfigSource.default.loadOrThrow[Config]))

      httpEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(config.httpThreads))
      dbEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(config.dbThreads))

      httpClient <- BlazeClientBuilder[IO](httpEC).resource

      retryPolicy = RetryPolicy[IO](
        RetryPolicy.exponentialBackoff(config.bitcoin.retryPolicy.delay, config.bitcoin.retryPolicy.attempts)
      )

      ethereumClient = new EthereumClient[IO](
        config.ethereum.node,
        chunkSize = config.bitcoin.chunkSize,
        maxConcurrent = config.bitcoin.maxConcurrent,
        Retry(retryPolicy)(
          Logger(logBody = false, logHeaders = true, logAction = Some((s: String) => IO.delay(logger.debug(s))))(
            httpClient
          )
        )
      )

      xa = Transactor.fromDriverManager[IO](
        config.postgres.driver,
        config.postgres.url,
        config.postgres.username,
        config.postgres.password,
        Blocker.liftExecutionContext(dbEC)
      )

      redis <- Redis[IO].utf8(config.redis.url)

      _ <- Resource.liftF(
        IO.delay(
          logger.info(s"Get blocks from ${config.bitcoin.fromBlock} to ${config.bitcoin.toBlock}")
        )
      )

      blocksAndTransactions = new BlocksAndTransactions[IO](ethereumClient, xa, redis)
      tokenTransfers = new TokenTransfers[IO](ethereumClient)
      postgresStats = new PostgresStats[IO](xa)
      redisStats = new RedisStats[IO](redis)

      // merge blocks and log stream for concurrency
      _ <- blocksAndTransactions
        .stream(config.ethereum.fromBlock, config.ethereum.toBlock)
        // .concurrently(tokenTransfers.stream(earliest, latest))
        .concurrently(postgresStats.stream("ethereum", config.ethereum.fromBlock, config.ethereum.toBlock))
        .concurrently(redisStats.stream("ethereum", config.ethereum.fromBlock, config.ethereum.toBlock))
        .compile
        .resource
        .drain

      _ <- Resource.liftF(
        redis
          .sCard("ethereum.blocks")
          .flatMap(count =>
            IO.delay(
              logger.info(s"Done! $count blocks are loaded.")
            )
          )
      )

      _ <- Resource.liftF(
        redis
          .sCard("ethereum.transactions")
          .flatMap(count =>
            IO.delay(
              logger.info(s"Done! $count transactions are loaded.")
            )
          )
      )
    } yield ()

    app.use(_ => IO.unit.as(ExitCode.Success))
  }
}
