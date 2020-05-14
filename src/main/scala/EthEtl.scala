package io.olownia

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger

import io.olownia.domain.eth._
import io.olownia.rpc.RpcClient
import io.olownia.streams._

object EthEtl extends IOApp with LazyLogging {
  val httpEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))

  override def run(args: List[String]): IO[ExitCode] = {
    val app = for {
      httpClient <- BlazeClientBuilder[IO](httpEC).resource

      rpcClient = new RpcClient[IO](
        "https://mainnet.infura.io/v3/b2e111d54d0b4992a341bad28cc363c5",
        chunkSize = 50,
        maxConcurrent = 4,
        Logger(logBody = false, logHeaders = true)(httpClient)
      )

      earliest <- Resource.liftF(
        rpcClient.one[Block.Params, Block](Block.request("earliest"))
      )
      latest <- Resource.liftF(
        rpcClient.one[Block.Params, Block](Block.request("latest"))
      )

      _ <- Resource.liftF(
        IO.delay(
          logger.info(s"Get blocks from ${earliest.number} to ${latest.number}")
        )
      )

      blocksAndTransactions = new BlocksAndTransactions[IO](rpcClient)
      tokenTransfers = new TokenTransfers[IO](rpcClient)

      // merge blocks and log stream for concurrency
      _ <- blocksAndTransactions
        .stream(earliest, latest)
        .concurrently(tokenTransfers.stream(earliest, latest))
        .compile
        .resource
        .drain
    } yield ()

    app.use(_ => IO.never.as(ExitCode.Success))
  }
}
