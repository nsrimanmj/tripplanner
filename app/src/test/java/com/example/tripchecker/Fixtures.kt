package com.example.tripchecker

object Fixtures {
    fun load(name: String): String {
        val path = "fixtures/$name"
        return Fixtures::class.java.classLoader!!.getResource(path)!!.readText()
            ?: error("Missing fixture: $path")
    }
}
