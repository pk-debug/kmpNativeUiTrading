/**
 * Collects debug-only Android window frame durations on a worker thread.
 * This diagnostic estimates frames exceeding the display interval; it is not
 * a substitute for release-build Macrobenchmark or compositor hitch traces.
 */
package com.pawan.kmpnativeuitrading.core.performance

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import java.util.concurrent.atomic.AtomicLong

internal class FrameDiagnostics(private val window: Window, refreshRate: Float) : AutoCloseable {
    private val frames = AtomicLong()
    private val slow = AtomicLong()
    private val droppedReports = AtomicLong()
    private val budgetNanos = (1_000_000_000.0 / refreshRate.coerceAtLeast(1f)).toLong()
    private val worker = HandlerThread("northstar-frame-diagnostics").apply { start() }
    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
        frames.incrementAndGet()
        if (metrics.getMetric(FrameMetrics.TOTAL_DURATION) > budgetNanos) slow.incrementAndGet()
        droppedReports.addAndGet(dropped.toLong())
    }

    init { window.addOnFrameMetricsAvailableListener(listener, Handler(worker.looper)) }

    /** Emit only aggregate timings, with no screen content, account IDs or credentials. */
    override fun close() {
        window.removeOnFrameMetricsAvailableListener(listener)
        worker.quitSafely()
        Log.i("NorthstarFrames", "frames=${frames.get()} overInterval=${slow.get()} droppedReports=${droppedReports.get()} budgetNs=$budgetNanos")
    }
}
