import org.codehaus.groovy.runtime.ProcessGroovyMethods
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

fun String.execute() = ProcessGroovyMethods.getText(ProcessGroovyMethods.execute(this)).trim()
