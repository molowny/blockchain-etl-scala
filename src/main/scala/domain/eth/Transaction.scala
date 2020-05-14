package io.olownia.domain.eth

import io.circe.{Encoder, Json}
import io.olownia.rpc.{RpcRequest, RpcResponse}

case class Transaction(
    hash: String
)

object Transaction {
  case class Params(hash: String)

  val rpcMethod = "eth_getTransactionByHash"

  type Request = RpcRequest[Params]
  type Response = RpcResponse[Transaction]

  def request(hash: String) =
    RpcRequest("2.0", rpcMethod, Params(hash), s"gtbh_$hash")

  implicit val encodeParams: Encoder[Params] = new Encoder[Params] {
    final def apply(params: Params): Json = Json.arr(
      Json.fromString(params.hash)
    )
  }
}
