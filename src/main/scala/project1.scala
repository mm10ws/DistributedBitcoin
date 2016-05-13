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

sealed trait Message
case class begin() extends Message
case class restart() extends Message
case class nbegin(address: String) extends Message
case class doWork(start: Int, nrOfElements: Int, zeros: Int) extends Message
case class newResult() extends Message

class Slave extends Actor {

  def receive = {
    case doWork(start, nrOfElements, zeros) =>

      find(start, nrOfElements, zeros)

      sender ! newResult() // perform the work
  }

  def find(start: Int, nrOfElements: Int, zeros: Int) = {
    val prefix = "mmehta10;"
    var zeroString = ""
    for (a <- 1 to zeros) {
      zeroString = zeroString + "0"
    }

    for (a <- 1 to nrOfElements) {
      val barray = doubleToByteArray(start + a)
      val b64 = Base64.getEncoder.encodeToString(barray)
      val s = prefix + b64
      val digest = MessageDigest.getInstance("SHA-256");
      val hash = digest.digest(s.getBytes("UTF-8"));
      val hexHash = toHex(hash)

      if (hexHash.startsWith(zeroString)) {
        println(s + "\t" + hexHash)
      }

    }

  }

  def doubleToByteArray(x: Int) = {
    ByteBuffer.allocate(4).putInt(x).array()
  }

  def toHex(b: Array[Byte]): String = {
    val builder = new StringBuilder()
    b.foreach { i =>
      builder.append(String.format("%02x", i: java.lang.Byte))

    }
    builder.toString()

  }

}

class Boss(numWorkers: Int, batchSize: Int, zeros: Int) extends Actor {
  var start: Int = 1

  val slaveRouter = context.actorOf(
    Props[Slave].withRouter(RoundRobinRouter(numWorkers)),
    name = "slaveRouter")

  def receive = {
    case begin() =>

      //make workers and give them jobs here

      for (a <- 1 to numWorkers) {
        //create workers

        slaveRouter ! doWork(start, batchSize, zeros)
        start = batchSize * a + 1

      }

    case newResult() =>

      sender ! doWork(start, batchSize, zeros)
      start = start + batchSize + 1

  }

}

class nSlave extends Actor {
  def receive = {
    case nbegin(address) =>

      val s = new Socket(InetAddress.getByName(address), 9999)
      lazy val in = new BufferedSource(s.getInputStream()).getLines()
      val out = new PrintStream(s.getOutputStream())

      out.println("hello")
      out.flush()
      val start = in.next()
      s.close()
      val numFound = find(start.toInt, 10000, 4, address)

      sender ! restart()

  }

  def find(start: Int, nrOfElements: Int, zeros: Int, address: String) = {
    val prefix = "mmehta10;"
    var zeroString = ""
    for (a <- 1 to zeros) {
      zeroString = zeroString + "0"
    }

    for (a <- 1 to nrOfElements) {
      val barray = doubleToByteArray(start + a)
      val b64 = Base64.getEncoder.encodeToString(barray)
      val s = prefix + b64
      val digest = MessageDigest.getInstance("SHA-256");
      val hash = digest.digest(s.getBytes("UTF-8"));
      val hexHash = toHex(hash)

      if (hexHash.startsWith(zeroString)) {
        val s1 = new Socket(InetAddress.getByName(address), 9999)
        val out = new PrintStream(s1.getOutputStream())
        out.println(s + "\t" + hexHash)
        out.flush()
        s1.close()

      }

    }

  }

  def doubleToByteArray(x: Int) = {
    ByteBuffer.allocate(4).putInt(x).array()
  }

  def toHex(b: Array[Byte]): String = {
    val builder = new StringBuilder()
    b.foreach { i =>
      builder.append(String.format("%02x", i: java.lang.Byte))

    }
    builder.toString()

  }

}

class nBoss(numWorkers: Int, batchSize: Int, address: String) extends Actor {
  val nslaveRouter = context.actorOf(
    Props[nSlave].withRouter(RoundRobinRouter(numWorkers)),
    name = "nslaveRouter")

  def receive = {
    case begin() =>

      //make workers and give them jobs here

      for (a <- 1 to numWorkers) {
        //create workers

        nslaveRouter ! nbegin(address)
      }

    case restart() =>

      sender ! nbegin(address)

  }

}

object project1 {

  // actors and messages ...

  def main(args: Array[String]) {
    // Create an Akka system

    if (args(0).contains(".")) {
      //network mode
      val system = ActorSystem("BitSystem")

      // create the master
      val nboss = system.actorOf(
        Props(new nBoss(8, 100000, args(0))),
        name = "nboss")

      // start the calculation
      nboss ! begin()

    } else {
      //normal mode
      val system = ActorSystem("BitSystem")

      // create the master
      val boss = system.actorOf(
        Props(new Boss(8, 100000, args(0).toInt)),
        name = "boss")

      // start the calculation
      boss ! begin()

    }

  }
}
