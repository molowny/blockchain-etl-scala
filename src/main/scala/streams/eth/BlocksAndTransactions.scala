package io.olownia.streams.eth

import cats.effect.{Async, Concurrent}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import dev.profunktor.redis4cats.RedisCommands
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._

import io.olownia.domain.ethereum._
import io.olownia.rpc.EthereumClient.GetBlockByNumber
import io.olownia.rpc.RpcClient

class BlocksAndTransactions[F[_]: Async: Concurrent](
    client: RpcClient[F],
    xa: Transactor[F],
    redis: RedisCommands[F, String, String]
) extends LazyLogging {
  def stream(from: Int, to: Int) =
    for {
      _ <- client
        .stream[GetBlockByNumber.Params, Block](
          Stream
            .range(from, to)
            .map(n => s"0x${n.toHexString}")
            .map(GetBlockByNumber.request)
        )
        .evalTap { block =>
          Async[F].delay(logger.debug(s"Save block: $block (transactions count: ${block.transactions.size})")) *>
            sql"insert into ethereum.blocks (hash, number) values (${block.hash}, ${block.number})".update.run.transact(
              xa
            ) *>
            redis.sAdd("ethereum.blocks", block.hash)
        }
        .map(_.transactions)
        .flatMap(Stream.emits)
        .evalTap { transaction =>
          Async[F].delay(logger.debug(s"Save transaction: $transaction")) *>
            sql"insert into ethereum.transactions (hash) values (${transaction.hash})".update.run.transact(xa) *>
            redis.sAdd("ethereum.transactions", transaction.hash)
        }
    } yield ()
}
