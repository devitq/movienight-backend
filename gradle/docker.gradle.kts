import org.gradle.api.tasks.Exec

/**
 * Usage:
 *  - ./gradlew dockerBuild
 *  - ./gradlew dockerPush
 */

val dockerImageName: String = (project.findProperty("dockerImageName") as String?) ?: run {
	val group = project.group?.toString()?.takeIf { it.isNotBlank() }
	val name = project.name.lowercase()
	val version = project.version?.toString()?.takeIf { it.isNotBlank() && it != "unspecified" } ?: "latest"
	if (group != null) {
		"${group}/${name}:${version}"
	} else {
		"${name}:${version}"
	}
}

tasks.register<Exec>("dockerBuild") {
	group = "docker"
	description = "Build Docker image using Containerfile"
	commandLine("docker", "build", "-f", "Containerfile", "-t", dockerImageName, ".")
}

tasks.register<Exec>("dockerPush") {
	group = "docker"
	description = "Push Docker image to registry (requires login)"
	onlyIf { System.getenv("CI") != null || project.hasProperty("pushDocker") }
	commandLine("docker", "push", dockerImageName)
}
