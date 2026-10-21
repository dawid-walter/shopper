dependencies {
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.4.0")
    implementation("io.github.resilience4j:resilience4j-micrometer:2.4.0")

    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
}
