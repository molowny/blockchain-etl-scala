package io.olownia

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.LazyLogging
import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import io.circe.generic.auto._

import io.olownia.domain.eth._
import io.olownia.rpc.RpcClient
import io.olownia.streams._

object EthEtl extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    val app = for {
      httpClient <- BlazeClientBuilder[IO](global).resource

      rpcClient = new RpcClient[IO](
        "http://localhost:8545",
        chunkSize = 50,
        maxConcurrent = 4,
        Logger(logBody = false, logHeaders = true)(httpClient)
      )

      earliest <- Resource.liftF(rpcClient.one[Block.Params, Block](Block.request("earliest")))
      latest <- Resource.liftF(rpcClient.one[Block.Params, Block](Block.request("latest")))

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
        .merge(tokenTransfers.stream(earliest, latest))
        .compile
        .resource
        .drain
    } yield ()

    app.use(_ => IO.never.as(ExitCode.Success))
  }
}
