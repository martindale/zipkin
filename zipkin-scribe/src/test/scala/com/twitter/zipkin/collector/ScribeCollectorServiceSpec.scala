/*
 * Copyright 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.twitter.zipkin.collector

import com.twitter.scrooge.BinaryThriftStructSerializer
import com.twitter.util.Future
import com.twitter.zipkin.collector.processor.Processor
import com.twitter.zipkin.common.{Span, Annotation}
import com.twitter.zipkin.config.sampler.AdjustableRateConfig
import com.twitter.zipkin.config.ScribeZipkinCollectorConfig
import com.twitter.zipkin.gen
import com.twitter.zipkin.adapter.ThriftAdapter
import java.util.concurrent.atomic.AtomicInteger
import org.specs.Specification
import org.specs.mock.{ClassMocker, JMocker}

class ScribeCollectorServiceSpec extends Specification with JMocker with ClassMocker {
  val serializer = new BinaryThriftStructSerializer[gen.Span] {
    def codec = gen.Span
  }
  val category = "zipkin"

  val validSpan = Span(123, "boo", 456, None, List(new Annotation(1, "bah", None)), Nil)
  val validList = List(gen.LogEntry(category, serializer.toString(ThriftAdapter(validSpan))))

  val wrongCatList = List(gen.LogEntry("wrongcat", serializer.toString(ThriftAdapter(validSpan))))

  val base64 = "CgABAAAAAAAAAHsLAAMAAAADYm9vCgAEAAAAAAAAAcgPAAYMAAAAAQoAAQAAAAAAAAABCwACAAAAA2JhaAAPAAgMAAAAAAA="

  val processor = mock[Processor[String]]
  val batch = mock[AtomicInteger]
  val zkSampleRateConfig = mock[AdjustableRateConfig]

  val config = new ScribeZipkinCollectorConfig {
    def writeQueueConfig = null
    def zkConfig = null
    def indexConfig = null
    def storageConfig = null
    def methodConfig = null

    override val maxQueueSize = 10
    override lazy val sampleRateConfig = zkSampleRateConfig
  }

  def scribeCollectorService = new ScribeCollectorService(config, processor, Set(category)) {
    running = true
  }

  "ScribeCollectorService" should {
    "add to queue" in {
      val cs = scribeCollectorService

      expect {
        one(processor).process(base64)
      }

      cs.log(validList)() mustEqual gen.ResultCode.Ok
    }

    "push back" in {
      val cs = new ScribeCollectorService(config, processor, Set(category)) {
        running = true
        override val batchCounter = new AtomicInteger(15)
      }

      expect {
        never(processor).process(base64)
      }

      cs.log(validList)() mustEqual gen.ResultCode.TryLater
    }

    "ignore wrong category" in {
      val cs = scribeCollectorService

      expect {
        never(processor).process(any)
      }

      cs.log(wrongCatList)() mustEqual gen.ResultCode.Ok
    }

    "get sample rate" in {
      val cs = scribeCollectorService

      val sampleRate = 0.3

      expect {
        one(zkSampleRateConfig).get willReturn sampleRate
      }

      val result = cs.getSampleRate
      result() mustEqual sampleRate
    }

    "set sample rate" in {
      val cs = scribeCollectorService

      val sampleRate = 0.4
      val expected = Future.Unit

      expect {
        one(zkSampleRateConfig).set(sampleRate)
      }

      val actual = cs.setSampleRate(sampleRate)
      actual() mustEqual expected()
    }
  }
}
