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

package org.polyvariant.scaladotfiles.scalafmt

import org.scalafmt.sbt.ScalafmtPlugin
import sbt.*

import Keys.*

/** An "sbt-github-actions–like" way to configure scalafmt: declare the version and settings as sbt
  * keys, and have the build generate a `.scalafmt.conf` from them.
  *
  * Unlike the Scalafix sibling, scalafmt is configured by a single file (no Compile/Test split),
  * so these keys are project-scoped rather than per-configuration.
  *
  * Enable explicitly: `.enablePlugins(ScalafmtConfigPlugin)`. Enabling the plugin means "I manage
  * `.scalafmt.conf` from the build here": the generated file is checked in and wired into
  * sbt-scalafmt's `scalafmtConfig`, so the IDE and the scalafmt CLI pick it up too.
  */
object ScalafmtConfigPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: Plugins = ScalafmtPlugin

  import ScalafmtPlugin.autoImport.scalafmtConfig

  object autoImport {

    val scalafmtConfiguredVersion = settingKey[String](
      "The scalafmt version, rendered as the mandatory `version` field in the generated config"
    )

    val scalafmtConfiguredSettings = settingKey[Map[String, Any]](
      "scalafmt settings, rendered as HOCON. Values may be String, Boolean, numbers, null, " +
        "Seq[Any] (lists) or nested Map[String, Any] (objects)."
    )

    val scalafmtConfiguredFile = settingKey[File](
      "Path of the generated .scalafmt.conf (checked in)"
    )

    val scalafmtConfiguredGenerate = taskKey[File](
      "Generate the .scalafmt.conf from the scalafmtConfigured* keys"
    )

    val scalafmtConfiguredCheck = taskKey[Unit](
      "Fail if the generated .scalafmt.conf is out of date with the scalafmtConfigured* keys"
    )

  }

  import autoImport.*

  // The scalafmt version bundled by the sbt-scalafmt this plugin builds against. scalafmt reads
  // its own version from the config file, and sbt-scalafmt exposes no key for it, so we default to
  // the bundled one and let users override.
  private val defaultScalafmtVersion = "3.10.0"

  override def projectSettings: Seq[Setting[?]] =
    Seq(
      scalafmtConfiguredVersion := defaultScalafmtVersion,
      scalafmtConfiguredSettings := Map.empty,
      scalafmtConfiguredFile := (LocalRootProject / baseDirectory).value / ".scalafmt.conf",
      scalafmtConfiguredGenerate := {
        val file = scalafmtConfiguredFile.value
        val contents = ScalafmtConfig.render(
          ScalafmtConfig(
            version = scalafmtConfiguredVersion.value,
            settings = scalafmtConfiguredSettings.value,
          )
        )
        IO.write(file, contents)
        streams.value.log.info(s"[scalafmtConfiguredGenerate] wrote $file")
        file
      },
      scalafmtConfiguredCheck := {
        val log = streams.value.log
        val file = scalafmtConfiguredFile.value
        val expected = ScalafmtConfig.render(
          ScalafmtConfig(
            version = scalafmtConfiguredVersion.value,
            settings = scalafmtConfiguredSettings.value,
          )
        )
        val actual =
          if (file.isFile)
            IO.read(file)
          else
            ""
        if (actual != expected)
          sys.error(
            s"[scalafmtConfiguredCheck] $file is out of date — run scalafmtConfiguredGenerate"
          )
        else
          log.info(s"[scalafmtConfiguredCheck] $file is up to date")
      },
      // Enabling the plugin means we own .scalafmt.conf: always point sbt-scalafmt at the
      // generated file. scalafmt has no useful "no config" mode (version is mandatory).
      scalafmtConfig := scalafmtConfiguredFile.value,
    )

}
