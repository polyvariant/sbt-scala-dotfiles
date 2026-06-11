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

class FileManagerTests extends munit.FunSuite {

  private val a = new File("a.txt")
  private val b = new File("sub/b.txt")

  // An in-memory map standing in for the disk, so the engine is tested without real IO.
  private final class FakeDisk(initial: Map[File, String] = Map.empty) {
    private var contents: Map[File, String] = initial
    val write: (File, String) => Unit = (f, s) => contents += (f -> s)
    val read: File => String = contents(_)
    val exists: File => Boolean = contents.contains(_)
    def snapshot: Map[File, String] = contents
  }

  test("generate writes every managed file") {
    val disk = new FakeDisk
    val managed = Map(a -> "hello\n", b -> "world\n")

    val written = FileManager.generate(managed, disk.write)

    assertEquals(disk.snapshot, managed)
    // returned files are sorted by path for stable logging
    assertEquals(written, Vector(a, b))
  }

  test("check is empty when on-disk content matches") {
    val managed = Map(a -> "hello\n", b -> "world\n")
    val disk = new FakeDisk(managed)

    assertEquals(FileManager.check(managed, disk.read, disk.exists), Vector.empty)
  }

  test("check reports a file whose content differs") {
    val managed = Map(a -> "hello\n")
    val disk = new FakeDisk(Map(a -> "tampered\n"))

    assertEquals(
      FileManager.check(managed, disk.read, disk.exists),
      Vector(FileManager.Stale(a, "hello\n", "tampered\n")),
    )
  }

  test("check treats a missing file as the empty string (stale)") {
    val managed = Map(a -> "hello\n")
    val disk = new FakeDisk // a never written

    assertEquals(
      FileManager.check(managed, disk.read, disk.exists),
      Vector(FileManager.Stale(a, "hello\n", "")),
    )
  }

}
