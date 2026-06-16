// `hoconFiles` (from HoconFilesPlugin) and `managedFiles*` (from the ManagedFilesPlugin it
// requires) are auto-imported by enabling the plugin below.

import com.typesafe.config.ConfigFactory

lazy val root = project
  .in(file("."))
  .enablePlugins(HoconFilesPlugin)
  .settings(
    scalaVersion := "2.13.18",
    // A representative HOCON tree: a scalar, a nested object and a list.
    hoconFiles := Map(
      baseDirectory.value / "app.conf" -> Map(
        "maxColumn" -> 100,
        "runner" -> Map("dialect" -> "scala213"),
        "rules" -> Seq("A", "B"),
      )
    ),
    // A raw, non-HOCON managed file declared alongside: the `++=` in the plugin must let both
    // coexist rather than one clobbering the other.
    managedFiles += (baseDirectory.value / "raw.txt") -> "literal\n",
  )

// --- assertions, run from the `test` script ---

val checkRendered = taskKey[Unit]("Rendered app.conf parses back to the configured HOCON tree")
checkRendered := {
  val txt = IO.read(baseDirectory.value / "app.conf")
  val parsed = ConfigFactory.parseString(txt)
  assert(parsed.getInt("maxColumn") == 100, s"maxColumn wrong in:\n$txt")
  assert(parsed.getString("runner.dialect") == "scala213", s"runner.dialect wrong in:\n$txt")
  val rules = parsed.getStringList("rules")
  assert(rules.size == 2 && rules.get(0) == "A" && rules.get(1) == "B", s"rules wrong in:\n$txt")
  // the HOCON layer adds no banner
  assert(!txt.contains("generated"), s"app.conf unexpectedly has a banner:\n$txt")
}

val checkRawCoexists = taskKey[Unit](
  "A raw managedFiles entry is generated alongside the HOCON one"
)

checkRawCoexists := {
  val raw = IO.read(baseDirectory.value / "raw.txt")
  assert(raw == "literal\n", s"raw.txt not verbatim: $raw")
}

val mutate = taskKey[Unit]("Corrupt the rendered HOCON file to force a staleness failure")
mutate := {
  val f = baseDirectory.value / "app.conf"
  IO.write(f, IO.read(f) + "\ntampered = true\n")
}
