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

import org.polyvariant.scaladotfiles.HoconConfig

/** A typed description of a `.scalafmt.conf` file: the mandatory scalafmt `version` plus arbitrary
  * free-form settings.
  *
  * Unlike Scalafix, scalafmt is configured by a single file (there's no Compile/Test split), and
  * the binary `version` is read from that file by scalafmt itself. We model it explicitly because
  * it is required; everything else (`maxColumn`, `runner.dialect`, `rewrite.rules`, …) goes into
  * `settings`.
  *
  * `settings` values may be any HOCON-representable shape: `String`, `Boolean`, numbers
  * (`Int`/`Long`/`Double`), `null`, `Seq[Any]` (lists), and nested `Map[String, Any]` (objects).
  * They are handed verbatim to the Typesafe config library, which renders them as HOCON.
  */
final case class ScalafmtConfig(
  version: String,
  settings: Map[String, Any],
)

object ScalafmtConfig {

  /** Banner prepended to every generated file, pointing back at the plugin. The generated file is
    * meant to be checked in, so this warns readers not to edit it by hand.
    */
  val header: String =
    HoconConfig.banner("sbt-scala-dotfiles-scalafmt", "scalafmtConfigured*", "scalafmtConfiguredGenerate")

  /** Render the config to the textual contents of a `.scalafmt.conf` file, including the [[header]]
    * banner.
    *
    * `version` is rendered first, ahead of the settings. Typesafe Config sorts an object's keys
    * alphabetically when rendering, so we can't rely on a single object to keep `version` on top;
    * instead `version` and the settings are rendered as separate top-level blocks and concatenated.
    */
  def render(config: ScalafmtConfig): String = {
    val versionBlock = HoconConfig.renderValue(Map("version" -> config.version))
    val settingsBlock =
      if (config.settings.isEmpty)
        ""
      else
        HoconConfig.renderValue(config.settings)

    header + "\n" + versionBlock + settingsBlock
  }

}
