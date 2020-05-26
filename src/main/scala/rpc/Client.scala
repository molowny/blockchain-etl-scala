package io.olownia.rpc

import scala.util.control.NoStackTrace

import cats.effect.Concurrent
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, EntityEncoder, Header, Method, Uri}

import io.olownia.rpc.RpcClient._

class RpcClient[F[_]: Concurrent](
    endpoint: String,
    chunkSize: Int,
    maxConcurrent: Int,
    client: Client[F],
    headers: Seq[Header] = Nil
) extends Http4sClientDsl[F]
    with LazyLogging {

  def one[P, R](request: RpcRequest[P])(
      implicit decode: EntityDecoder[F, RpcResponse[R]],
      encode: EntityEncoder[F, RpcRequest[P]]
  ): F[R] =
    client
      .fetchAs[RpcResponse[R]](
        Method.POST(request, Uri.unsafeFromString(endpoint), headers: _*)
      )
      .flatMap {
        case RpcResponse(_, _, Some(error), _) =>
          Concurrent[F].raiseError(error)
        case RpcResponse(_, Some(result), _, _) =>
          Concurrent[F].pure(result)
        case _ =>
          Concurrent[F].raiseError(
            new UnsupportedOperationException(
              "Empty response result and error."
            )
          )
      }

  def stream[P, R](request: Stream[F, RpcRequest[P]])(
      implicit decode: EntityDecoder[F, Seq[RpcResponse[R]]],
      encode: EntityEncoder[F, List[RpcRequest[P]]]
  ): Stream[F, R] =
    request
      .chunkN(chunkSize)
      .mapAsyncUnordered(maxConcurrent) { chunks =>
        client
          .fetchAs[Seq[RpcResponse[R]]](
            Method.POST(chunks.toList, Uri.unsafeFromString(endpoint), headers: _*)
          )
      }
      .flatMap(Stream.emits)
      .flatMap {
        case RpcResponse(_, _, Some(error), _) =>
          Stream.raiseError[F](error)
        case RpcResponse(_, Some(result), _, _) =>
          Stream(result)
        case _ =>
          Stream.raiseError[F](
            new UnsupportedOperationException(
              "Empty response result and error."
            )
          )
      }

}

object RpcClient {
  // case class RpcId(id: String)

  case class RpcException(
      code: Int,
      message: String,
      data: Option[String]
  ) extends RuntimeException(s"Json-RPC error $code: $message")
      with NoStackTrace

  case class RpcRequest[P](
      jsonrpc: String,
      method: String,
      params: P,
      id: String
  )

  case class RpcResponse[R](
      jsonrpc: Option[String],
      result: Option[R],
      error: Option[RpcException],
      id: String
  )
}
