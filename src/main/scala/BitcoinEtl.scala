package io.olownia

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.LazyLogging
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout._
import doobie.util.transactor.Transactor
import org.http4s.BasicCredentials
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{Logger, Retry, RetryPolicy}
import org.http4s.headers.Authorization
import pureconfig._
import pureconfig.generic.auto._

import io.olownia.rpc.BitcoinClient
import io.olownia.streams._
import io.olownia.streams.bitcoin._

object BitcoinEtl extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    val app = for {
      config <- Resource.liftF(IO.delay(ConfigSource.default.loadOrThrow[Config]))

      httpEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(config.httpThreads))
      dbEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(config.dbThreads))

      httpClient <- BlazeClientBuilder[IO](httpEC).resource

      retryPolicy = RetryPolicy[IO](
        RetryPolicy.exponentialBackoff(config.bitcoin.retryPolicy.delay, config.bitcoin.retryPolicy.attempts)
      )

      bitcoinClient = new BitcoinClient[IO](
        config.bitcoin.node,
        chunkSize = config.bitcoin.chunkSize,
        maxConcurrent = config.bitcoin.maxConcurrent,
        Retry(retryPolicy)(
          Logger(logBody = false, logHeaders = true, logAction = Some((s: String) => IO.delay(logger.debug(s))))(
            httpClient
          )
        ),
        Seq(Authorization(BasicCredentials(config.bitcoin.username, config.bitcoin.password)))
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

      blocksAndTransactions = new BlocksAndTransactions[IO](bitcoinClient, xa, redis)
      postgresStats = new PostgresStats[IO](xa)
      redisStats = new RedisStats[IO](redis)

      // merge blocks and log stream for concurrency
      _ <- blocksAndTransactions
        .stream(config.bitcoin.fromBlock, config.bitcoin.toBlock)
        .concurrently(postgresStats.stream("bitcoin", config.bitcoin.fromBlock, config.bitcoin.toBlock))
        .concurrently(redisStats.stream("bitcoin", config.bitcoin.fromBlock, config.bitcoin.toBlock))
        .compile
        .resource
        .drain

      _ <- Resource.liftF(
        redis
          .sCard("bitcoin.blocks")
          .flatMap(count =>
            IO.delay(
              logger.info(s"Done! $count blocks are loaded.")
            )
          )
      )

      _ <- Resource.liftF(
        redis
          .sCard("bitcoin.transactions")
          .flatMap(count =>
            IO.delay(
              logger.info(s"Done! $count transactions are loaded.")
            )
          )
      )
    } yield ()

    // app.use(_ => IO.never.as(ExitCode.Success))
    app.use(_ => IO.unit.as(ExitCode.Success))
  }
}
