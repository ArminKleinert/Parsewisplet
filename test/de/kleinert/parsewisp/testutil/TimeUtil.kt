package de.kleinert.parsewisp.testutil

import de.kleinert.parsewisp.functions.Procedure
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Utilities for measuring runtime performance.
 */
object TimeUtil {
    /**
     * Runs a procedure a few times and creates some statistics. Time is represented as milliseconds.
     *
     * @param n How often to run the procedure.
     * @param f The procedure.
     * @return The analysis result as a string.
     */
    fun measureTimeMillis(n: Int, f: Procedure): String {
        require(n >= 1)

        val stream = generateSequence({
            val start = System.nanoTime()
            f()
            val end = System.nanoTime()
            (end - start) / 1000000.0
        }).take(n).sorted().toList()

        val min = stream[0]
        val max = stream[stream.size - 1]
        val diff = max - min
        val sum = stream.sum()
        val avg = sum / n
        val mid = stream[stream.size / 2]
        val median = (stream[n / 4] + stream[(n / 4) * 3]) / 2

        return String.format(
            "{:lowest %.3f, :highest %.3f, :diff %.3f, :average %.3f, :mid %.3f, :median %.3f, :total %.3f}",
            min, max, diff, avg, mid, median, sum
        )
    }

    /**
     * Helper method for tests.
     * @param n Number of iterations.
     * @param f Code to run.
     * @return The average time in milliseconds. Outlier times are removed.
     */
    fun measureTimeMillisMean(n: Int, f: Procedure): Double {
        require(n >= 1)

        val stream = generateSequence({
            val start = System.nanoTime()
            f()
            val end = System.nanoTime()
            (end - start) / 1000000.0
        }).take(n).sorted().toList()

        val factor = 0.1
        val keep = ceil((1 - factor) * stream.size).toInt()
        return stream.drop((floor(stream.size * factor) / 2).toInt()).take(keep).sum() / keep
    }

    /**
     * Another test method.
     *
     * @param milliseconds Time limit.
     * @param f            Code to execute.
     * @return Number of executions within the time limit.
     */
    fun measureExecutionsPer(milliseconds: Long, f: Procedure): String {
        val startTime = System.nanoTime()
        val endTime = startTime + milliseconds * 1000000
        var last: Long
        var executions: Long = 0

        while ((System.nanoTime().also { last = it }) < endTime) {
            f()
            executions++
        }

        val actualDuration = last - startTime

        return "Executions: " + executions + "; Actual duration (ms): " + (actualDuration / 1000000.0)
    }
}
