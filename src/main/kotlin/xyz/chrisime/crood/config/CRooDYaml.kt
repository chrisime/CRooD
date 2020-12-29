package xyz.chrisime.crood.config

data class CRooDYaml(val serialization: Serialization? = null) {
    data class Serialization(val annotations: Annotations? = null) {
        data class Annotations(val transient: Boolean? = null)
    }
}
