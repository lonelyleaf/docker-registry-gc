package xyz.lonelyleaf.docker.registry.gc

data class TagResponse(
    val name: String,
    val tags: List<String>
)