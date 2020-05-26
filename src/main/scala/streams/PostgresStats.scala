package io.olownia.streams

import scala.concurrent.duration._

import cats.effect.{Async, Timer}
import com.typesafe.scalalogging.LazyLogging
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream

class PostgresStats[F[_]: Async: Timer](xa: Transactor[F]) extends LazyLogging {
  def stream(schema: String, from: Int, to: Int) =
    for {
      startTime <- Stream
        .eval(Timer[F].clock.monotonic(MILLISECONDS))

      stats = sql"select count(*) from ${Fragment.const(schema)}.blocks"
        .query[Int]
        .stream
        .take(1)
        .transact(xa)
        .zip(Stream.eval(Timer[F].clock.monotonic(MILLISECONDS)))
        .evalMap {
          case (count, currentTime) =>
            Async[F].delay {
              val total = to - from
              val remaining = total - count
              val speed = (currentTime - startTime).toDouble / count
              val percentage = count.toDouble / total * 100
              logger.info(s"$count / ${total}, ${"%1.2f".format(percentage)}%, ${"%1.2f"
                .format(1000 / speed)} records/s, estimated time remaining: ${Duration(remaining * speed, MILLISECONDS).toMinutes} minutes")
            }
        }
        .repeat

      _ <- Stream.awakeEvery[F](5.second).zipRight(stats)
    } yield ()
}
