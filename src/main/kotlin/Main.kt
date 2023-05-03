val studentsUsername = ""
    .split("\n")
val studentsPassword = ""
    .split("\n")

fun main() {
    studentsUsername.forEachIndexed { index, username ->
        val student = Student(username,studentsPassword[index])
        student.start()
    }
}
