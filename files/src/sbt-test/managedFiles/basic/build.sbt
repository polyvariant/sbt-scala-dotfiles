// `managedFiles*` (from ManagedFilesPlugin) is auto-imported by enabling the plugin below.

val aContent = "hello\n"
val bContent = "world\n"

lazy val root = project
  .in(file("."))
  .enablePlugins(ManagedFilesPlugin)
  .settings(
    scalaVersion := "2.13.18",
    // A flat file and one in a nested directory: generate must create the directory too.
    managedFiles := Map(
      baseDirectory.value / "a.txt" -> aContent,
      baseDirectory.value / "sub" / "b.txt" -> bContent,
    ),
  )

// --- assertions, run from the `test` script ---

val checkExactNoBanner = taskKey[Unit]("Files are written verbatim — exact content, no banner")
checkExactNoBanner := {
  val a = IO.read(baseDirectory.value / "a.txt")
  val b = IO.read(baseDirectory.value / "sub" / "b.txt")
  assert(a == aContent, s"a.txt not verbatim: ${a.mkString}")
  assert(b == bContent, s"b.txt not verbatim: ${b.mkString}")
  assert(!a.contains("generated"), s"a.txt unexpectedly has a banner:\n$a")
}

val mutate = taskKey[Unit]("Corrupt a managed file to force a staleness failure")
mutate := {
  val f = baseDirectory.value / "a.txt"
  IO.write(f, IO.read(f) + "tampered\n")
}

val deleteOne = taskKey[Unit]("Delete a managed file — a missing file must count as out of date")
deleteOne :=
  IO.delete(baseDirectory.value / "a.txt")
