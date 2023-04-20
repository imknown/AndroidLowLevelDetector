import org.gradle.api.provider.ProviderFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun getCurrentDatetime(): String {
    val pattern = "yyyyMMdd-HHmm"
    val formatter = DateTimeFormatter.ofPattern(pattern)
    val instant = Instant.now()
    val datetime = instant.atZone(ZoneId.systemDefault())
    return formatter.format(datetime)
}

fun ProviderFactory.execute(vararg args: Any) =
    exec { commandLine(*args) }.standardOutput.asText.get().trim()