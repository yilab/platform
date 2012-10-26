/*
 *  ____    ____    _____    ____    ___     ____ 
 * |  _ \  |  _ \  | ____|  / ___|  / _/    / ___|        Precog (R)
 * | |_) | | |_) | |  _|   | |     | |  /| | |  _         Advanced Analytics Engine for NoSQL Data
 * |  __/  |  _ <  | |___  | |___  |/ _| | | |_| |        Copyright (C) 2010 - 2013 SlamData, Inc.
 * |_|     |_| \_\ |_____|  \____|   /__/   \____|        All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU Affero General Public License as published by the Free Software Foundation, either version 
 * 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See 
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this 
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.precog.common.json

import org.specs2.mutable.Specification

import blueeyes.json.JsonAST._
import blueeyes.json.serialization.{ Decomposer, Extractor, ValidatedExtraction }
import blueeyes.json.serialization.DefaultSerialization._

import shapeless._

class SerializationSpec extends Specification {
  case class Foo(s: String, i: Option[Int], b: Boolean)
  implicit val fooIso = Iso.hlist(Foo.apply _, Foo.unapply _)
  val foo = Foo("Hello world", Some(23), true)
  val foo2 = Foo("Hello world", None, true)
  val fooSchema = "s" :: "i" :: "b" :: HNil
  val safeFooSchema = "s" :: Omit :: "b" :: HNil
  
  case class Bar(d: Double, f: Foo, l: List[String])
  implicit val barIso = Iso.hlist(Bar.apply _, Bar.unapply _)
  val bar = Bar(2.3, foo, List("foo", "bar", "baz"))
  val barSchema = "d" :: "f" :: "l" :: HNil
  val inlinedBarSchema = "d" :: Inline :: "l" :: HNil
  
  case class Baz(s: String, l: List[Foo])
  implicit val bazIso = Iso.hlist(Baz.apply _, Baz.unapply _)
  val baz = Baz("Hello world", List(foo, foo2))
  val bazSchema = "s" :: "l" :: HNil
  
  "serialization" should {
    "serialize a simple case class" in {
      val fooDecomp = decomposer[Foo](fooSchema)
      
      val result = fooDecomp.decompose(foo)
      
      result must_==
        JObject(List(
          JField("s", "Hello world"),
          JField("i", 23),
          JField("b", true)
        ))
    }
    
    "serialize a simple case class omitting absent optional fields" in {
      val fooDecomp = decomposer[Foo](fooSchema)
      
      val result = fooDecomp.decompose(foo2)
      
      result must_==
        JObject(List(
          JField("s", "Hello world"),
          JField("b", true)
        ))
    }
    
    "serialize a simple case class with omitted fields" in {
      val fooDecomp = decomposer[Foo](safeFooSchema)
      
      val result = fooDecomp.decompose(foo)
      
      result must_==
        JObject(List(
          JField("s", "Hello world"),
          JField("b", true)
        ))
    }
    
    "serialize a case class with a nested case class element" in {
      implicit val fooDecomp = decomposer[Foo](fooSchema)
      val barDecomp = decomposer[Bar](barSchema)
      
      val result = barDecomp.decompose(bar)
      
      result must_==
        JObject(List(
          JField("d", 2.3),
          JField("f",
            JObject(List(
              JField("s", "Hello world"),
              JField("i", 23),
              JField("b", true)
            ))),
          JField("l",
            JArray(List("foo", "bar", "baz")))
        ))
    }
    
    "serialize a case class with a nested case class element respecting alternative schema" in {
      implicit val fooDecomp = decomposer[Foo](safeFooSchema)
      val barDecomp = decomposer[Bar](barSchema)
      
      val result = barDecomp.decompose(bar)
      
      result must_==
        JObject(List(
          JField("d", 2.3),
          JField("f",
            JObject(List(
              JField("s", "Hello world"),
              JField("b", true)
            ))),
          JField("l",
            JArray(List("foo", "bar", "baz")))
        ))
    }
    
    "serialize a case class with an inlined case class element" in {
      implicit val fooDecomp = decomposer[Foo](fooSchema)
      val barDecomp = decomposer[Bar](inlinedBarSchema)

      val result = barDecomp.decompose(bar)
      
      result must_==
        JObject(List(
          JField("d", 2.3),
          JField("s", "Hello world"),
          JField("i", 23),
          JField("b", true),
          JField("l",
            JArray(List("foo", "bar", "baz")))
        ))
    }

    "serialize a case class with a list of nested case class elements" in {
      implicit val fooDecomp = decomposer[Foo](fooSchema)
      val bazDecomp = decomposer[Baz](bazSchema)

      val result = bazDecomp.decompose(baz)
      
      result must_==
        JObject(List(
          JField("s", "Hello world"),
          JField("l",
            JArray(List(
              JObject(List(
                JField("s", "Hello world"),
                JField("i", 23),
                JField("b", true)
              )),
              JObject(List(
                JField("s", "Hello world"),
                JField("b", true)
              ))
            )))
        ))
    }
  }
}