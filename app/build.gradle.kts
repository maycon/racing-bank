// Root build.gradle.kts

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
