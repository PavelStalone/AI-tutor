package rut.uvp.family.log

object Log {

    fun e(throwable: Throwable, message: String) {
        println("[ERROR] $throwable $message")
    }

    fun i(message: String) {
        println("[INFO] $message")
    }

    fun d(message: String) {
        println("[DEBUG] $message")
    }

    fun v(message: String) {
        println("[TRACE] $message")
    }
}
