package xyz.lonelyleaf.docker.registry.gc

data class TagList(
    val name: String,
    val tags: List<String>
)