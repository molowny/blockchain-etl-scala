package io.olownia.streams.bitcoin

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

import io.olownia.domain.bitcoin._
import io.olownia.rpc.BitcoinClient

class BlocksAndTransactions[F[_]: Async: Concurrent](
    client: BitcoinClient[F],
    xa: Transactor[F],
    redis: RedisCommands[F, String, String]
) extends LazyLogging {
  def stream(from: Int, to: Int): Stream[F, Unit] =
    for {

      _ <- client
        .stream[BitcoinClient.GetBlockHash.Params, String](
          Stream
            .range(from, to)
            .map(BitcoinClient.GetBlockHash.request)
        )
        .map(BitcoinClient.GetBlock.request)
        .through(client.stream[BitcoinClient.GetBlock.Params, Block])
        .evalTap { block =>
          Async[F].delay(logger.debug(s"Save block: $block (tx count: ${block.tx.size})")) *>
            sql"insert into bitcoin.blocks (hash) values (${block.hash})".update.run.transact(xa) *>
            redis.sAdd("bitcoin.blocks", block.hash)
        }
        .map(_.tx)
        .flatMap(Stream.emits)
        .evalTap { transaction =>
          Async[F].delay(logger.debug(s"Save transaction: $transaction")) *>
            sql"insert into bitcoin.transactions (hash) values (${transaction.hash})".update.run.transact(xa) *>
            redis.sAdd("bitcoin.transactions", transaction.hash)
        }
    } yield ()
}
