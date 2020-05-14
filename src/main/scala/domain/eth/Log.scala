package io.olownia.domain.eth

import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.olownia.rpc.{RpcRequest, RpcResponse}

case class Log(
    address: String,
    blockHash: String,
    blockNumber: String,
    data: String,
    logIndex: String,
    removed: Boolean,
    topics: Seq[String],
    transactionHash: String,
    transactionIndex: String
)

object Log {
  case class Params(
      fromBlock: String,
      toBlock: String,
      topics: Seq[String]
  )

  val rpcMethod = "eth_getLogs"

  // https://ethereum.stackexchange.com/questions/12553/understanding-logs-and-log-blooms
  // ERC20 token transfer event
  val transferEventTopic =
    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"

  type Request = RpcRequest[Params]
  type Response = RpcResponse[Transaction]

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
