import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util._
import java.math.BigInteger
import java.util.Base64
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.net._
import java.io._
import scala.io._

sealed trait sMessage
case class sbegin() extends sMessage

class nServer extends Actor {
  var start: Int = 1

  def receive = {
    case sbegin() =>

      val server = new ServerSocket(9999)
      while (true) {
        val s = server.accept()
        val in = new BufferedSource(s.getInputStream()).getLines()
        val out = new PrintStream(s.getOutputStream())
        val input = in.next

        if (input.equals("hello")) {
          out.println(start)
          start = start + 10000
        } else {
          println(input)
        }

        out.flush()
        s.close()
      }

  }

}

object server {
  def main(args: Array[String]) {
    val system = ActorSystem("nBitSystem")
    val nserver = system.actorOf(
      Props(new nServer()),
      name = "nserver")

    nserver ! sbegin()

  }

}