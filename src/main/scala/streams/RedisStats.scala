package io.olownia.streams

import scala.concurrent.duration._

import cats.effect.{Async, Timer}
import com.typesafe.scalalogging.LazyLogging
import dev.profunktor.redis4cats.RedisCommands
import fs2.Stream

class RedisStats[F[_]: Async: Timer](redis: RedisCommands[F, String, String]) extends LazyLogging {
  def stream(schema: String, from: Int, to: Int) =
    for {
      startTime <- Stream
        .eval(Timer[F].clock.monotonic(MILLISECONDS))

      stats = Stream
        .eval(redis.sCard(s"$schema.blocks"))
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
