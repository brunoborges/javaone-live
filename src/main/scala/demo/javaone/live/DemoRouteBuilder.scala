package demo.javaone.live

import org.apache.camel.Exchange
import org.apache.camel.scala.dsl.builder.RouteBuilder
import org.apache.camel.component.twitter.TwitterComponent
import twitter4j.Status
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy
import org.apache.camel.model.dataformat.JsonDataFormat
import org.apache.camel.model.dataformat.JsonLibrary

class DemoRouteBuilder extends RouteBuilder with ConfigureComponents {

  val jsonFormat = new JsonDataFormat(JsonLibrary.Jackson)
  val UNIQUE_IMAGE = "UNIQUE_IMAGE"

  Statistics.keywords = configProperties.getProperty("twitter.searchTerm")

  "twitter://streaming/filter?type=event&keywords=" + Statistics.keywords ==> {
    process(StatisticsProcessor)
    when(_.in.asInstanceOf[Status].getMediaEntities() != null) {
      setHeader(UNIQUE_IMAGE, _.in.asInstanceOf[Status].getMediaEntities()(0).getMediaURL().getFile())
      process(StatusToTweetConverter)
      marshal(jsonFormat)
      to("websocket:0.0.0.0:8080/javaone/images?sendToAll=true")
    }
  }

  "quartz:statistics?cron=* * * * * ?" ==> {
    setBody(Statistics)
    marshal(jsonFormat)
    to("websocket:0.0.0.0:8080/javaone/statistics?sendToAll=true")
  }

}