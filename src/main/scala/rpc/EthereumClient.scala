package io.olownia.rpc

import cats.effect.Concurrent
import doobie.util.Write
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.client._
import org.http4s.{EntityDecoder, EntityEncoder, Header}

import io.olownia.domain.ethereum.Block
import io.olownia.rpc.EthereumClient._
import io.olownia.rpc.RpcClient._

class EthereumClient[F[_]: Concurrent](
    endpoint: String,
    chunkSize: Int,
    maxConcurrent: Int,
    client: Client[F],
    headers: Seq[Header] = Nil
) extends RpcClient(endpoint, chunkSize, maxConcurrent, client, headers) {
  def getBlock(number: String)(
      implicit decode: EntityDecoder[F, RpcResponse[Block]],
      encode: EntityEncoder[F, RpcRequest[GetBlockByNumber.Params]]
  ) =
    one[GetBlockByNumber.Params, Block](
      GetBlockByNumber.request(number)
    )
}

object EthereumClient {
  object GetBlockByNumber {
    val rpcMethod = "eth_getBlockByNumber"
    case class Params(number: String, fullTransactions: Boolean)
    def request(number: String) = RpcRequest("2.0", rpcMethod, Params(number, true), s"gbbn_$number")

    implicit val encodeParams: Encoder[Params] = new Encoder[Params] {
      final def apply(params: Params): Json = Json.arr(
        Json.fromString(params.number),
        Json.fromBoolean(params.fullTransactions)
      )
    }

    implicit val write: Write[Block] =
      Write[(String, String)].contramap(p => (p.hash, p.number))
  }

  object GetTransactionByHash {
    val rpcMethod = "eth_getTransactionByHash"
    case class Params(hash: String)
    def request(hash: String) =
      RpcRequest("2.0", rpcMethod, Params(hash), s"gtbh_$hash")

    implicit val encodeParams: Encoder[Params] = new Encoder[Params] {
      final def apply(params: Params): Json = Json.arr(
        Json.fromString(params.hash)
      )
    }
  }

  object Logs {
    val rpcMethod = "eth_getLogs"
    case class Params(
        fromBlock: String,
        toBlock: String,
        topics: Seq[String]
    )

    // https://ethereum.stackexchange.com/questions/12553/understanding-logs-and-log-blooms
    // ERC20 token transfer event
    val transferEventTopic =
      "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"

    def getEcr20TokenTransfers(fromBlock: String, toBlock: String) =
      RpcRequest(
        "2.0",
        rpcMethod,
        Params(fromBlock, toBlock, Seq(transferEventTopic)),
        s"gl_ecr20_token_transfer_${fromBlock}_${toBlock}"
      )

    implicit val encodeParams: Encoder[Params] = new Encoder[Params] {
      final def apply(params: Params): Json = Json.arr(
        Json.obj(
          "fromBlock" -> Json.fromString(params.fromBlock),
          "toBlock" -> Json.fromString(params.toBlock),
          "topics" -> params.topics.asJson
        )
      )
    }
  }
}
