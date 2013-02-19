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

import actor._
import metadata._
import util._
import SValue._
import com.precog.common._
import com.precog.common.accounts._
import com.precog.common.ingest._
import com.precog.common.security._
import com.precog.common.util._
import com.precog.common.json._
import com.precog.util._

import akka.actor.ActorSystem
import akka.dispatch._
import akka.testkit.TestActorRef
import akka.util.Timeout
import akka.util.duration._

import blueeyes.json._

import scalaz._
import scalaz.effect._
import scalaz.syntax.std.boolean._

import scala.collection.immutable.SortedMap
import scala.collection.immutable.TreeMap

class StubStorageMetadata[M[+_]](projectionMetadata: Map[Path, Map[ColumnRef, Long]])(implicit M: Monad[M]) extends StorageMetadata[M]{
  def findDirectChildren(path: Path) = M point {
    projectionMetadata.keySet collect {
      case key if key.isChildOf(path) => Path(key.components(path.length))
    }
  }

  def findSize(path: Path) = M.point(0L)
  def findSelectors(path: Path) = M.point(projectionMetadata.getOrElse(path, Map.empty[ColumnRef, Long]).keySet.map(_.selector))
  def findStructure(path: Path, selector: CPath) = M.point {
    val types: Map[CType, Long] = projectionMetadata.getOrElse(path, Map.empty[ColumnRef, Long]) collect {
      case (ColumnRef(`selector`, ctype), count) => (ctype, count)
    }

    val children = projectionMetadata.getOrElse(path, Map.empty[ColumnRef, Long]) flatMap {
      case (ColumnRef(s, ctype), count) => if (s.hasPrefix(selector)) s.take(selector.length) else None
    }

    PathStructure(types, children.toSet)
  }
}

trait StubProjectionModule[M[+_], Key, Block] extends ProjectionModule[M, Key, Block] { self =>
  implicit def M: Monad[M]

  protected def projections: Map[Path, Projection]

  class ProjectionCompanion extends ProjectionCompanionLike[M] {
    def apply(path: Path) = M.point(projections.get(path))
  }
}
