package xyz.chrisime.crood.config

import kotlinx.serialization.*

@Serializable
data class CRooDYaml(val serialization: Serialization) {
    @Serializable
    data class Serialization(val annotations: Annotations) {
        @Serializable
        data class Annotations(val transient: Boolean)
    }
}
