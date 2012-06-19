
package com.twitter.zipkin.hadoop

/**
 * Created with IntelliJ IDEA.
 * User: jli
 * Date: 6/18/12
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */

import org.specs.Specification
import com.twitter.zipkin.gen
import com.twitter.scalding._
import gen.AnnotationType
import sources.SpanSource
import scala.collection.JavaConverters._
import collection.mutable.HashMap
import java.nio.ByteBuffer

class MemcacheRequestSpec extends Specification with TupleConversions {

  noDetailedDiffs()

  implicit val dateRange = DateRange(RichDate(123), RichDate(321))

  val endpoint = new gen.Endpoint(123, 666, "service")
  val endpoint1 = new gen.Endpoint(123, 666, "service1")
  val endpoint2 = new gen.Endpoint(123, 666, "service2")
  val span = new gen.Span(12345, "methodcall", 666,
    List(new gen.Annotation(1000, "cs").setHost(endpoint), new gen.Annotation(2000, "cr").setHost(endpoint)).asJava,
    List[gen.BinaryAnnotation]().asJava)
  val span1 = new gen.Span(123456, "methodcall", 666,
    List(new gen.Annotation(1000, "cs").setHost(endpoint1), new gen.Annotation(4000, "cr").setHost(endpoint1)).asJava,
    List(new gen.BinaryAnnotation("memcached.keys", ByteBuffer.allocate(4).putInt(0, 10), AnnotationType.BOOL)).asJava)
  val span2 = new gen.Span(1234567, "methodcall", 666,
    List(new gen.Annotation(1000, "cs").setHost(endpoint2), new gen.Annotation(3000, "cr").setHost(endpoint2)).asJava,
    List(new gen.BinaryAnnotation("memcached.keys", ByteBuffer.allocate(4).putInt(0, 10), AnnotationType.BOOL)).asJava)


  "MemcacheRequest" should {
    "print out some shit" in {
      JobTest("com.twitter.zipkin.hadoop.MemcacheRequest").
        arg("input", "inputFile").
        arg("output", "outputFile").
        arg("date", "2012-01-01T01:00").
        source(SpanSource(), (repeatSpan(span2, 10, 0) ++ repeatSpan(span1, 20, 100))).
        sink[(String, String, Long)](Tsv("outputFile")) {
        outputBuffer => outputBuffer foreach { e =>
          println(e)
        }
        //        counts("service") mustEqual 100
        //        counts("service1") mustEqual 51
      }.run.finish
    }
  }

  def repeatSpan(span: gen.Span, count: Int, offset : Int): List[(gen.Span, Int)] = {
    ((0 to count).toSeq map { i: Int => span.deepCopy().setId(i + offset) -> (i + offset)}).toList
  }
}
