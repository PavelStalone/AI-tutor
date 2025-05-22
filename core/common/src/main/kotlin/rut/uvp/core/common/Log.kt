package rut.uvp.core.common.log

object Log {

    fun i(message: String) {
        println("[INFO] $message")
    }

    fun d(message: String) {
        println("[DEBUG] $message")
    }

    fun v(message: String) {
        println("[TRACE] $message")
    }

    fun e(throwable: Throwable, message: String) {
        println("[ERROR] $throwable $message")
    }
}
