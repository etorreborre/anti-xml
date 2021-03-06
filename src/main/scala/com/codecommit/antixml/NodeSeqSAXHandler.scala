/*
 * Copyright (c) 2011, Daniel Spiewak
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer. 
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.codecommit
package antixml

import util._

import org.xml.sax.Attributes
import org.xml.sax.ext.DefaultHandler2

class NodeSeqSAXHandler extends DefaultHandler2 {
  private var elems = List[Group[Node] => Elem]()
  private val text = new StringBuilder
  private var isCDATA = false
  
  private var builders = VectorCase.newBuilder[Node] :: Nil
  
  override def startCDATA() {
    clearText()
    isCDATA = true
  }
  
  override def endCDATA() {
    clearText()
    isCDATA = false
  }
  
  override def characters(ch: Array[Char], start: Int, length: Int) {
    text.appendAll(ch, start, length)
  }
  
  override def startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
    clearText()
    
    builders ::= VectorCase.newBuilder
    elems ::= { children =>
      val ns = if (uri == "") None else Some(uri)
      val map = (0 until attrs.getLength).foldLeft(Map[String, String]()) { (map, i) =>
        map + (attrs.getQName(i) -> attrs.getValue(i))    // TODO namespacing
      }
      
      Elem(ns, localName, map, children)
    }
  }

  override def endElement(uri: String, localName: String, qName: String) {
    clearText()
    
    val (build :: elems2) = elems
    elems = elems2
    
    val result = build(pop())
    builders.head += result
  }
  
  override def skippedEntity(entity: String) {
    clearText()
    builders.head += EntityRef(entity)
  }
  
  def result() = pop().asInstanceOf[Group[Elem]]       // nasty, but it shouldn't be a problem
  
  private def pop() = {
    val (back :: builders2) = builders
    builders = builders2
    Group fromSeq back.result
  }
  
  private def clearText() {
    val construct = if (isCDATA) CDATA else Text
    
    if (!text.isEmpty) {
      builders.head += construct(text.toString)
      text.clear()
    }
  }
} 
