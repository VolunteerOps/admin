package com.example.admin.core.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private fun statePath(): Path = Path.of(System.getProperty("volunteerops.dataDir", System.getProperty("user.home") + "/.volunteerops"), "admin.state")
actual fun readLocalState(): String? = statePath().let { if (Files.exists(it)) Files.readString(it) else null }
actual fun writeLocalState(value: String) {
    val target = statePath()
    Files.createDirectories(target.parent)
    val pending = Files.createTempFile(target.parent, "admin", ".pending")
    Files.writeString(pending, value)
    try { Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
    catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING) }
}
