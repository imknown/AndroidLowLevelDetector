import org.codehaus.groovy.runtime.ProcessGroovyMethods
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun getCurrentDatetime(): String? {
    val now = Instant.now().atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(now)
}

fun String.execute() = ProcessGroovyMethods.getText(ProcessGroovyMethods.execute(this)).trim()
