package com.abdulradi.opentracing.xray.v1.jaeger.senders

import java.net.{DatagramPacket, DatagramSocket, InetAddress}

import scala.util.{Failure, Success, Try}

import com.uber.jaeger.Span
import com.uber.jaeger.exceptions.SenderException
import com.uber.jaeger.senders.Sender
import io.circe.Encoder

// TODO: I've no idea what the int return supposed to mean,
// I assume it is a sort of a count, since errors are propagated as Exceptions
class AgentBased[T: Encoder](agentAddress: InetAddress, agentPort: Int, converter: Span => Either[String, T]) extends Sender {
  private var buffer = Vector.empty[T]
  val socket = new DatagramSocket()
  val encoder = implicitly[Encoder[T]]

  override def append(span: Span): Int =
    converter(span) match {
      case Right(trace) => buffer = buffer :+ trace; buffer.size
      case Left(error) => throw new SenderException(error, new Exception(error), 1)
    }

  // TODO: Should retry the failed ones? Should I at least keep them in buffer? For now they are simply dropped down the drain
  override def flush(): Int = {
    val (maybeError, failureCount, successCount) = buffer.map(send).foldLeft[(Option[Throwable], Int, Int)]((None, 0, 0)) {
      case ((accError, accFailureCount, accSuccessCount), Success(_)) => (accError, accFailureCount, accSuccessCount + 1)
      case ((accError, accFailureCount, accSuccessCount), Failure(e)) => (accError.orElse(Some(e)), accFailureCount + 1, accSuccessCount)
    }
    buffer = Vector.empty[T] // yikes mutability!
    maybeError.fold(successCount)(error => throw new SenderException(error.getMessage, error, failureCount))
  }

  private def send(trace: T): Try[Unit] =
    encoder(trace).as[Array[Byte]].fold(
      failure => Failure(failure),
      data => Try(socket.send(new DatagramPacket(data, data.length, agentAddress, agentPort)))
    )

  override def close(): Int =
    Try(socket.close()).fold(
      error => throw new SenderException(error.getMessage, error, buffer.size),
      _ => buffer.size
    )
}

object AgentBased {
  def apply[T: Encoder](agentAddress: String, agentPort: Int, converter: Span => Either[String, T]) =
    new AgentBased(InetAddress.getByName(agentAddress), agentPort, converter)

//  TODO: Implement Encocder[TopLevelTrace]
//  def default() =
//    apply("localhost", 2000, SpanConverter)
}