package kurstest

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume


fun main() {
    val context = EmptyCoroutineContext

    val completionContinuation = Continuation<String?>(context) { result ->
        println("Continuation completed with: $result")
    }

    runImmediatelyOneTooMany(completionContinuation)
}

fun runImmediately(completionContinuation: Continuation<String?>) {
    val invocationContinuation = MyTestContinuation(completionContinuation)
    invocationContinuation.resume(Unit)
    invocationContinuation.resume(Unit)
}

fun runImmediatelyWithSleep(completionContinuation: Continuation<String?>) {
    val invocationContinuation = MyTestContinuation(completionContinuation)
    invocationContinuation.resume(Unit)
    Thread.sleep(2000)
    invocationContinuation.resume(Unit)
}

fun runImmediatelyWithAsyncDelay(completionContinuation: Continuation<String?>) {
    val executorService = Executors.newSingleThreadScheduledExecutor()

    try {
        val invocationContinuation = MyTestContinuation(completionContinuation)
        invocationContinuation.resume(Unit)
        executorService.schedule({ invocationContinuation.resume(Unit) }, 2, TimeUnit.SECONDS)
    } finally {
        executorService.shutdown()
    }
}

fun runImmediatelyOneTooMany(completionContinuation: Continuation<String?>) {
    val invocationContinuation = MyTestContinuation(completionContinuation)
    invocationContinuation.resume(Unit)
    invocationContinuation.resume(Unit)
    invocationContinuation.resume(Unit)
}

object COROUTINE_SUSPENDED

class MyTestContinuation(val completion: Continuation<String>) : Continuation<Unit> {
    override val context: CoroutineContext
        get() = completion.context

    var result: Result<Unit>? = null
    var label = 0

    override fun resumeWith(result: Result<Unit>) {
        this.result = result
        val res = try {
            val r = myTestFunction(this)
            if (r === COROUTINE_SUSPENDED) return
            Result.success(r as String)
        } catch (e: Throwable) {
            Result.failure(e)
        }
        completion.resumeWith(res)
    }
}

fun myTestFunction(continuation: MyTestContinuation): Any {
    try {
        if (continuation.label == 0) {
            println("First invocation")
            return COROUTINE_SUSPENDED
        }

        if (continuation.label == 1) {
            println("Second invocation!")
            return "My continuation result!"
        }

        error("This should never happen")
    } finally {
        continuation.label++
    }

}