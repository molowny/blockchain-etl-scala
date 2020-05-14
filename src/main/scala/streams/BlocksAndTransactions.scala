package io.olownia.streams

import cats.effect.Async
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._

import io.olownia.domain.eth._
import io.olownia.rpc.RpcClient

class BlocksAndTransactions[F[_]: Async](client: RpcClient[F]) extends LazyLogging {
  def stream(from: Block, to: Block) =
    for {
      fromNumber <- Stream
        .emit(from)
        .map(_.number)
        .map(Integer.decode)

      toNumber <- Stream
        .emit(to)
        .map(_.number)
        .map(Integer.decode)

      blocks = Stream
        .range(toNumber, fromNumber, -1) // get blocks form latest to earliest
        .map(n => s"0x${n.toHexString}")
        .map(Block.request)

      block <- client
        .stream[Block.Params, Block](blocks)
        .evalTap { block => Async[F].delay(logger.info(s"Save block ${block.number} to db")) }

      // transactions
      transactions = Stream(block.transactions)
        .flatMap(Stream.emits)
        .map(Transaction.request)

      _ <- client
        .stream[Transaction.Params, Transaction](transactions)
        .evalTap { transaction => Async[F].delay(logger.info(s"Save transaction ${transaction.hash} to db")) }
    } yield ()
}
