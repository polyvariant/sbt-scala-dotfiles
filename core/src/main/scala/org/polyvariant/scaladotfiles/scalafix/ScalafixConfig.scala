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

package org.polyvariant.scaladotfiles.scalafix

import org.polyvariant.scaladotfiles.HoconConfig

/** A typed description of a `.scalafix.conf` file: the list of rules to run plus arbitrary per-rule
  * settings.
  *
  * `settings` values may be any HOCON-representable shape: `String`, `Boolean`, numbers
  * (`Int`/`Long`/`Double`), `null`, `Seq[Any]` (lists), and nested `Map[String, Any]` (objects).
  * They are handed verbatim to the Typesafe config library, which renders them as HOCON.
  */
final case class ScalafixConfig(
  rules: Seq[String],
  settings: Map[String, Any],
)

object ScalafixConfig {

  /** Banner prepended to every generated file, pointing back at the plugin (cf.
    * sbt-github-actions). The generated file is meant to be checked in, so this warns readers not
    * to edit it by hand.
    */
  val header: String =
    HoconConfig.banner(
      "sbt-scala-dotfiles-scalafix",
      "scalafixConfigured*",
      "scalafixConfiguredGenerate",
    )

  import HoconConfig.renderValue

  /** Render the config to the textual contents of a `.scalafix.conf` file, including the [[header]]
    * banner.
    *
    * `rules` is rendered first, ahead of the per-rule settings. Typesafe Config sorts an object's
    * keys alphabetically when rendering, so we can't rely on a single object to keep `rules` on
    * top; instead `rules` and the settings are rendered as separate top-level blocks and
    * concatenated.
    */
  def render(config: ScalafixConfig): String = {
    val rulesBlock = renderValue(Map("rules" -> config.rules))
    val settingsBlock =
      if (config.settings.isEmpty)
        ""
      else
        renderValue(config.settings)

    header + "\n" + rulesBlock + settingsBlock
  }

}
