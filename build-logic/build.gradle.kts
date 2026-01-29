tasks.register<Delete>("clean") {
    finalizedBy(":convention:clean")
}