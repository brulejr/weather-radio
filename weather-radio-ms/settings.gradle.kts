rootProject.name = "weather-radio-ms"

val useLocalKsbCommons =
    providers.gradleProperty("useLocalKsbCommons")
        .map { it.toBoolean() }
        .orElse(true)
        .get()

val ksbCommonsDir = file("vendor/ksb-commons")

if (useLocalKsbCommons) {
    if (ksbCommonsDir.exists()) {
        includeBuild(ksbCommonsDir)
    } else {
        logger.warn("useLocalKsbCommons=true but ./vendor/ksb-commons is missing; falling back to published dependency")
    }
}