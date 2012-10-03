package demo.javaone.live

import org.apache.camel.model.dataformat.JsonDataFormat
import org.apache.camel.model.dataformat.JsonLibrary
import org.apache.camel.scala.dsl.builder.RouteBuilder
import demo.javaone.live.util._
import twitter4j.Status
import org.apache.camel.component.websocket.WebsocketConstants

class DemoRouteBuilder extends RouteBuilder with ConfigureComponents {

  val lruImages = new FifoBlockingQueue[Tweet](12)
  val jsonFormat = new JsonDataFormat(JsonLibrary.Jackson)
  val UNIQUE_IMAGE = "UNIQUE_IMAGE"

  Statistics.keywords = configProperties.getProperty("twitter.searchTerm")

  "twitter://streaming/filter?type=event&keywords=" + Statistics.keywords ==> {
    process(StatisticsProcessor)
    when(_.in.asInstanceOf[Status].getMediaEntities() != null) {
      process(StatusToTweetConverter)
      process(exchange ⇒ {
        lruImages.offer(exchange.getIn().getBody().asInstanceOf[Tweet])
      })
      marshal(jsonFormat)
      to("websocket:0.0.0.0:8080/javaone/images?sendToAll=true")
    }
  }

  "quartz:statistics?cron=* * * * * ?" ==> {
    setBody(Statistics)
    marshal(jsonFormat)
    to("websocket:0.0.0.0:8080/javaone/statistics?sendToAll=true")
  }

  "websocket:0.0.0.0:8080/javaone/sample" ==> {
    split(lruImages) {
    	marshal(jsonFormat)
    	to("websocket:0.0.0.0:8080/javaone/sample")
    }
  }

}