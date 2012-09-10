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
package com.precog.yggdrasil
package table

import org.specs2.mutable.Specification
import org.specs2.ScalaCheck

import blueeyes.json.JPathField
import blueeyes.json.JsonAST._
import blueeyes.json.JsonParser

import scalaz._
import scalaz.syntax.copointed._

trait UnionAllSpec[M[+_]] extends ColumnarTableModuleTestSupport[M] with Specification with ScalaCheck {
  import Table._

  override type GroupId = Int

  def simpleUnionAllTest = {
    val JArray(left) = JsonParser.parse("""[
      {
        "groupKeys":  { "001": "foo", "002": false },
        "identities": { "1": [1,2] },
        "values":     { "1": { "a": "foo", "b": false } }
      },
      {
        "groupKeys":  { "001": "foo", "002": true },
        "identities": { "1": [1,3] },
        "values":     { "1": { "a": "foo", "b": true } }
      }
    ]""")

    val JArray(right) = JsonParser.parse("""[
      {
        "groupKeys":  { "001": "bar", "002": true },
        "identities": { "1": [5,1] },
        "values":     { "1": { "a": "bar", "b": true } }
      },
      {
        "groupKeys":  { "001": "baz", "002": true },
        "identities": { "1": [4,5] },
        "values":     { "1": { "a": "baz", "b": true } }
      }
    ]""")

    val vars = Seq(JPathField("a"), JPathField("b"))
  
    val leftBorg = BorgResult(fromJson(left.toStream), vars, Set(1))
    val rightBorg = BorgResult(fromJson(right.toStream), vars, Set(1))

    val expected = left.toStream ++ right.toStream

    val result = unionAll(Set(leftBorg, rightBorg))
    val jsonResult = result.table.toJson.copoint

    jsonResult       must_== expected
    result.groupKeys must_== vars
    result.groups    must_== Set(1)
  }
}
