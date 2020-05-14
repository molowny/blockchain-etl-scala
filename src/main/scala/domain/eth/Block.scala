package io.olownia.domain.eth

import io.circe.{Encoder, Json}
import io.olownia.rpc.{RpcRequest, RpcResponse}

case class Block(
    hash: String,
    number: String,
    transactions: Seq[String]
)

object Block {
  case class Params(block: String, fullTransactions: Boolean)

  val rpcMethod = "eth_getBlockByNumber"

  type Request = RpcRequest[Params]
  type Response = RpcResponse[Block]

  def request(block: String) =
    RpcRequest("2.0", rpcMethod, Params(block, false), s"gbbn_$block")

  implicit val encodeParams: Encoder[Params] = new Encoder[Params] {
    final def apply(params: Params): Json = Json.arr(
      Json.fromString(params.block),
      Json.fromBoolean(params.fullTransactions)
    )
  }
}
