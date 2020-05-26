package io.olownia.rpc

import cats.effect.Concurrent
import doobie.util.Write
import io.circe.{Encoder, Json}
import org.http4s.client._
import org.http4s.{EntityDecoder, EntityEncoder, Header}

import io.olownia.domain.bitcoin.Block
import io.olownia.rpc.BitcoinClient._
import io.olownia.rpc.RpcClient._

class BitcoinClient[F[_]: Concurrent](
    endpoint: String,
    chunkSize: Int,
    maxConcurrent: Int,
    client: Client[F],
    headers: Seq[Header] = Nil
) extends RpcClient(endpoint, chunkSize, maxConcurrent, client, headers) {
  def getBlock(hash: String)(
      implicit decode: EntityDecoder[F, RpcResponse[Block]],
      encode: EntityEncoder[F, RpcRequest[GetBlock.Params]]
  ) =
    one[GetBlock.Params, Block](
      GetBlock.request(hash)
    )
}

object BitcoinClient {
  object GetBlockHash {
    val rpcMethod = "getblockhash"
    case class Params(height: Int)
    def request(height: Int) = RpcRequest("1.0", rpcMethod, Params(height), s"gbh_$height")
  }

  object GetBlock {
    val rpcMethod = "getblock"
    case class Params(hash: String, verbosity: Int)
    def request(hash: String) = RpcRequest("1.0", rpcMethod, Params(hash, 2), s"gb_$hash")

    implicit val encodeParams: Encoder[Params] = new Encoder[Params] {
      final def apply(params: Params): Json = Json.arr(
        Json.fromString(params.hash),
        Json.fromInt(params.verbosity)
      )
    }

    implicit val write: Write[Block] =
      Write[(String, Int)].contramap(p => (p.hash, p.height))
  }
}
