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
package com.twitter.zipkin.collector.processor

import com.twitter.ostrich.stats.Stats
import com.twitter.zipkin.collector.sampler.GlobalSampler
import com.twitter.zipkin.common.Span

/**
 * Filters out `Span`s that do not meet a `GlobalSampler`'s criteria
 * @param sampler
 */
class SamplerProcessorFilter(sampler: GlobalSampler) extends ProcessorFilter[Option[Span], Option[Span]] {
  def apply(span: Option[Span]): Option[Span] = {
    span match {
      case Some(s) => {
        s.serviceNames.foreach { name => Stats.incr("received_" + name) }

        if (sampler(s.traceId)) {
          span
        } else {
          None
        }
      }
      case _ => None
    }
  }
}
