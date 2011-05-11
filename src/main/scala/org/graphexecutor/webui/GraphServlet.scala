package org.graphexecutor.webui

import scala.collection.mutable.Set
import java.io.{ OutputStream, PrintStream }
import javax.servlet.http._
import org.eclipse.jetty.websocket._
import org.eclipse.jetty.websocket.WebSocket.Outbound

class GraphServlet extends WebSocketServlet {
  val clients = Set.empty[GraphWebSocket]

  var classpathstringer = "whatever" //System.getProperty("replhtml.class.path")

  override def doGet( req: HttpServletRequest, res: HttpServletResponse ) = {
    println( req )
    println( res )
    getServletContext.getNamedDispatcher( "default" ).forward( req, res )
  }

  override def doWebSocketConnect( req: HttpServletRequest, protocol: String ) =
    new GraphWebSocket

  class WebSocketPrintStream( cls: Set[GraphWebSocket] ) extends PrintStream( new OutputStream { def write( b: Int ) = {} } ) {

    override def print( message: String ) = {
      cls.foreach { c => c.outbound.sendMessage( 0: Byte, message ) }
    }

    override def println( message: String ) = {
      clients.foreach { c => c.outbound.sendMessage( 0: Byte, message + "\n\r" ) }
    }
    override def println() = {
      clients.foreach { c => c.outbound.sendMessage( 0: Byte, "\n\r" ) }
    }

  }

  class GraphWebSocket extends WebSocket {

    var outbound: Outbound = _

    override def onConnect( outbound: Outbound ) = {
      println( "GraphWebSocket onConnect" + outbound )
      this.outbound = outbound
      clients += this
    }

    override def onMessage( frame: Byte, data: Array[Byte], offset: Int, length: Int ) = {
      println( "GraphWebSocket->onMessage : frame:" + frame + " data:" + data + " lenght:" + length )
    }

    override def onMessage( frame: Byte, data: String ) = {
      println( "GraphWebSocket->onMessage : frame:" + frame + " data:" + data )
    }

    override def onDisconnect = clients -= this

  }
}
