package org.graphexecutor.webui
import java.io.File


import javax.servlet.http._
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.DefaultServlet

object WebUIMain {
  def main(args: Array[String]) {
    val server = new Server(8080)

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS) //(ServletContextHandler.NO_SESSIONS SESSIONS)
    context.setContextPath("/")

    context.addServlet(new ServletHolder(new GraphServlet()),"/socket/*")
    context.addServlet(new ServletHolder(new DefaultServlet()),"/*")
    server.setHandler(context)

    println( System.getProperty("user.dir") )
    println( System.getProperty("jetty.home","../jetty-distribution/target/distribution") )
    
    println("ContextHandlers: "+context.getHandlers)
    println("ContextAttributes: "+context.getBaseResource)
    println("ContextPath: "+context.getContextPath)
    
    server.start()
    println(">>> embedded jetty server started. press any key to stop.")
    while (System.in.available() == 0) {
      Thread.sleep(1500)
    }
    System.in.read()
    println(">>> stopping...")
    server.stop()
    server.join()
  }
}