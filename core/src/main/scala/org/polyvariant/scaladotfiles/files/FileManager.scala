/*
 * Copyright 2026 Polyvariant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polyvariant.scaladotfiles.files

import java.io.File

/** The shared "write-and-check generated files" engine, used by every plugin in this build (the
  * generic `managedFiles` engine, the HOCON layer, and the scalafix/scalafmt tools). It operates on
  * a plain `Map[File, String]` of target path to exact content.
  *
  * IO is injected (`write`/`read`/`exists`) rather than performed here, so this stays a pure,
  * sbt-free helper that `core` can hold and unit-test without an sbt classpath. The sbt plugins
  * pass sbt's `IO.write` / `IO.read` / `_.isFile`.
  */
object FileManager {

  /** Write every managed file verbatim. Returns the files written, sorted by path for stable,
    * deterministic logging.
    */
  def generate(managed: Map[File, String], write: (File, String) => Unit): Seq[File] = {
    val files = managed.keys.toVector.sortBy(_.getPath)
    files.foreach(f => write(f, managed(f)))
    files
  }

  /** A managed file whose on-disk content does not match what would be generated. */
  final case class Stale(file: File, expected: String, actual: String)

  /** Report every managed file that is out of date. A missing file is compared as the empty string
    * (so a never-generated file shows up as stale). Returns the mismatches rather than throwing, so
    * each caller can format its own tool-specific error message.
    */
  def check(managed: Map[File, String], read: File => String, exists: File => Boolean): Seq[Stale] =
    managed.toVector.sortBy(_._1.getPath).flatMap { case (file, expected) =>
      val actual =
        if (exists(file))
          read(file)
        else
          ""
      if (actual != expected)
        Some(Stale(file, expected, actual))
      else
        None
    }

}
