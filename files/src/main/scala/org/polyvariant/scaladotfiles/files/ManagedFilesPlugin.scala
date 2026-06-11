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

import sbt.*

import Keys.*

/** The most general plugin in this build: declare a set of files as `target -> exact content` and
  * have the build generate them and check them for staleness. Everything else here — the HOCON
  * layer, the scalafix/scalafmt tools — is a specialization that ultimately produces such a map.
  *
  * Content is written verbatim: no banner, no transformation. If you want a "do not edit by hand"
  * header, include it in the string yourself (the tool layers do).
  *
  * Enable explicitly: `.enablePlugins(ManagedFilesPlugin)`.
  */
object ManagedFilesPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: Plugins = empty

  object autoImport {

    val managedFiles = settingKey[Map[File, String]](
      "Files to maintain, as target path -> exact content (written verbatim, no banner)"
    )

    val managedFilesGenerate = taskKey[Seq[File]](
      "Write every managed file verbatim"
    )

    // A single project-scoped map already covers all of a project's files, so there's no
    // `*All` task here — the `*All` pair only exists in the scalafix plugin because it splits
    // a project's config across Compile/Test.
    val managedFilesCheck = taskKey[Unit](
      "Fail if any managed file is out of date (a missing file counts as out of date)"
    )

  }

  import autoImport.*

  override def projectSettings: Seq[Setting[?]] =
    Seq(
      managedFiles := Map.empty,
      managedFilesGenerate := {
        val log = streams.value.log
        val written = FileManager.generate(managedFiles.value, IO.write(_, _))
        written.foreach(f => log.info(s"[managedFilesGenerate] wrote $f"))
        written
      },
      managedFilesCheck := {
        val log = streams.value.log
        val stale = FileManager.check(managedFiles.value, IO.read(_), _.isFile)
        if (stale.nonEmpty)
          sys.error(
            "[managedFilesCheck] out of date — run managedFilesGenerate:\n" +
              stale.map(s => s"  - ${s.file}").mkString("\n")
          )
        else
          log.info("[managedFilesCheck] all managed files up to date")
      },
    )

}
