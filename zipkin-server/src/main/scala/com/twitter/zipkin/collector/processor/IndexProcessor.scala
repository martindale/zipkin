/*
 * Copyright 2012 Twitter Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.zipkin.collector.processor

import com.twitter.zipkin.storage.Index
import com.twitter.zipkin.collector.filter.IndexingFilter
import com.twitter.zipkin.common.Span
import com.twitter.util.Future

/**
 * Index the incoming spans.
 */
class IndexProcessor(index: Index, indexFilter: IndexingFilter) extends Processor[Option[Span]] {

  def process(span: Option[Span]) = {
    span match {
      case Some(s) if indexFilter.shouldIndex(s) =>
        Future.join(Seq {
          index.indexTraceIdByServiceAndName(s) onFailure failureHandler("indexTraceIdByServiceAndName")
          index.indexSpanByAnnotations(s)       onFailure failureHandler("indexSpanByAnnotations")
          index.indexServiceName(s)             onFailure failureHandler("indexServiceName")
          index.indexSpanNameByService(s)       onFailure failureHandler("indexSpanNameByService")
          index.indexSpanDuration(s)            onFailure failureHandler("indexSpanDuration")
        })
      case _ => Future.Unit
    }
  }

  def shutdown() = index.close()
}
