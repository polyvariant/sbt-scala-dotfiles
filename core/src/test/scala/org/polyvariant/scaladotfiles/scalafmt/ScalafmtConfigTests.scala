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

import com.typesafe.config.ConfigFactory

class ScalafmtConfigTests extends munit.FunSuite {

  // Portable across Scala 2.12/3: turn a java.util.List into a Scala List without
  // scala.jdk / JavaConverters (which differ between those versions).
  private def toScala[A](xs: java.util.List[A]): List[A] = {
    val b = List.newBuilder[A]
    val it = xs.iterator()
    while (it.hasNext)
      b += it.next()
    b.result()
  }

  // Modeled on a real-world .scalafmt.conf (this very repo's): a version plus a mix of scalars,
  // dotted keys, a nested object and a list of rule names.
  private val sample = ScalafmtConfig(
    version = "3.11.1",
    settings = Map(
      "runner" -> Map(
        "dialect" -> "sbt1",
        "dialectOverride" -> Map(
          "allowSignificantIndentation" -> false,
          "allowQuietSyntax" -> true,
        ),
      ),
      "maxColumn" -> 100,
      "align" -> Map("preset" -> "some"),
      "trailingCommas" -> "multiple",
      "rewrite" -> Map(
        "rules" -> Seq("RedundantBraces", "RedundantParens", "ExpandImportSelectors")
      ),
    ),
  )

  test("render includes the banner header pointing at the plugin") {
    val out = ScalafmtConfig.render(sample)
    assert(out.startsWith(ScalafmtConfig.header), s"missing header in:\n$out")
    assert(out.contains("sbt-scala-dotfiles-scalafmt plugin"))
  }

  test("rendered output parses back to an equivalent structure (round-trip)") {
    val out = ScalafmtConfig.render(sample)
    val parsed = ConfigFactory.parseString(out)

    assertEquals(parsed.getString("version"), "3.11.1")
    assertEquals(parsed.getString("runner.dialect"), "sbt1")
    assertEquals(parsed.getBoolean("runner.dialectOverride.allowSignificantIndentation"), false)
    assertEquals(parsed.getBoolean("runner.dialectOverride.allowQuietSyntax"), true)
    assertEquals(parsed.getInt("maxColumn"), 100)
    assertEquals(parsed.getString("align.preset"), "some")
    assertEquals(parsed.getString("trailingCommas"), "multiple")
    assertEquals(
      toScala(parsed.getStringList("rewrite.rules")),
      List("RedundantBraces", "RedundantParens", "ExpandImportSelectors"),
    )
  }

  test("version is rendered before the settings") {
    val out = ScalafmtConfig.render(sample)
    val body = out.stripPrefix(ScalafmtConfig.header)
    assert(
      body.indexOf("version") < body.indexOf("maxColumn"),
      s"version should come before settings in:\n$out",
    )
  }

  test("regex string with backslashes round-trips exactly") {
    val cfg = ScalafmtConfig(
      version = "3.11.1",
      settings = Map("project" -> Map("excludeFilters" -> Seq("re:.*\\.sbt"))),
    )
    val parsed = ConfigFactory.parseString(ScalafmtConfig.render(cfg))
    assertEquals(
      toScala(parsed.getStringList("project.excludeFilters")),
      List("re:.*\\.sbt"),
    )
  }

  test("nested objects render and round-trip") {
    val cfg = ScalafmtConfig(
      version = "3.11.1",
      settings = Map("a" -> Map("b" -> Map("c" -> 42))),
    )
    val parsed = ConfigFactory.parseString(ScalafmtConfig.render(cfg))
    assertEquals(parsed.getInt("a.b.c"), 42)
  }

  test("empty settings renders just the version") {
    val parsed = ConfigFactory.parseString(
      ScalafmtConfig.render(ScalafmtConfig(version = "3.11.1", settings = Map.empty))
    )
    assertEquals(parsed.getString("version"), "3.11.1")
    assertEquals(parsed.entrySet().size(), 1)
  }

}
