package xyz.lonelyleaf.docker.registry.gc

data class Catalog(
        /**
         * list of image
         */
        val repositories: List<String>
)