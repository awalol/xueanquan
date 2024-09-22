val studentsUsername = ""
val studentsPassword = ""

fun main() {
    studentsUsername.split("\n").forEachIndexed { _, username ->
        println("------Current Student: $username")
        val student = Student(username,studentsPassword)
        student.start()
    }
}
