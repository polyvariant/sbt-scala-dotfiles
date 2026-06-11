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

package org.polyvariant.scaladotfiles.hocon

import org.polyvariant.scaladotfiles.HoconConfig
import org.polyvariant.scaladotfiles.files.ManagedFilesPlugin
import sbt.*

/** Maintain arbitrary HOCON files: declare `target -> value tree` and have each tree rendered to
  * HOCON and fed into the [[ManagedFilesPlugin]] engine. A specialization of the generic
  * `managedFiles` engine for the (common) case where the file is HOCON.
  *
  * Like the generic engine, no banner is added — the rendered HOCON body is the file content. The
  * scalafix/scalafmt tools add their own banner before handing content to the engine.
  *
  * Enable explicitly: `.enablePlugins(HoconFilesPlugin)`. Generate/check the files through the
  * engine's `managedFilesGenerate` / `managedFilesCheck`.
  */
object HoconFilesPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: Plugins = ManagedFilesPlugin

  // Read/write the engine's key qualified rather than re-exporting it from this plugin's
  // autoImport: re-exporting would give a build enabling both plugins two wildcard paths to the
  // same `managedFiles` name and an ambiguous-reference error.
  import ManagedFilesPlugin.autoImport.managedFiles

  object autoImport {

    val hoconFiles = settingKey[Map[File, Map[String, Any]]](
      "HOCON files to maintain, as target path -> value tree. Each tree is rendered to HOCON " +
        "(no banner) and added to `managedFiles`. Values may be String, Boolean, numbers, null, " +
        "Seq[Any] (lists) or nested Map[String, Any] (objects)."
    )

  }

  import autoImport.*

  override def projectSettings: Seq[Setting[?]] =
    Seq(
      hoconFiles := Map.empty,
      // `++=` so raw `managedFiles` entries and other layers compose rather than clobber.
      managedFiles ++= hoconFiles.value.map { case (file, tree) =>
        file -> HoconConfig.renderValue(tree)
      },
    )

}
