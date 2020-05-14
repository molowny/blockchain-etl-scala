package io.olownia.streams

import com.typesafe.scalalogging.LazyLogging
import cats.effect.Async
import fs2.Stream
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import io.circe.generic.auto._

import io.olownia.rpc.RpcClient
import io.olownia.domain.eth._

class TokenTransfers[F[_]: Async](client: RpcClient[F]) extends LazyLogging {
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

      logs = Stream
        .range(toNumber, fromNumber - 1, -1) // get log form latest to earliest
        .map(n => s"0x${n.toHexString}")
        .chunkN(20)
        .map { chunk =>
          for {
            head <- chunk.head
            last <- chunk.last
          } yield Log.getEcr20TokenTransfers(last, head)
        }
        .collect {
          case Some(request) => request
        }
        .evalTap { log => Async[F].delay(logger.info(s"Get log for: $log")) }

      _ <- client
        .stream[Log.Params, Seq[Log]](logs)
        .flatMap(Stream.emits)
        .evalTap { log =>
          Async[F].delay(logger.info(s"Extract transaction form log: $log"))
        }
    } yield ()
}
