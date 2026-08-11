package corp.khin.solutions.booqi

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform