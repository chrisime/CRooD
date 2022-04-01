package xyz.chrisime.crood.config

data class CRooDConfig(val annotations: Annotations, val frameworks: Frameworks)

data class Annotations(val useTransient: Boolean, val useJakarta: Boolean)
data class Frameworks(val isMicronaut: Boolean)
