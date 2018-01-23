package com.abdulradi.opentracing.xray.v1.senders

import java.net.{DatagramPacket, DatagramSocket, InetAddress}

import scala.util.Try

import io.circe.syntax._
import io.circe.{Encoder, Printer}

class AgentBasedSender(agentAddress: InetAddress, agentPort: Int) {
  private val socket = new DatagramSocket()
  private val printer = Printer.noSpaces.copy(dropNullValues = true)

  def send[T: Encoder](trace: T): Try[Unit] = {
    val data = s"""{"format": "json", "version": 1}\n${printer.pretty(trace.asJson)}""".getBytes
    Try(socket.send(new DatagramPacket(data, data.length, agentAddress, agentPort)))
  }

  def close(): Try[Unit] =
    Try(socket.close())
}

object AgentBasedSender {
  def apply(agentAddress: String, agentPort: Int) =
    new AgentBasedSender(InetAddress.getByName(agentAddress), agentPort)
}
