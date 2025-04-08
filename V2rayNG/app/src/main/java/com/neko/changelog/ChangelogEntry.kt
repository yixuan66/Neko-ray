package com.neko.changelog

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)