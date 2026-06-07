package com.mapmate.telemetry

/**
 * Làm mượt speed bằng rolling average (trung bình trượt).
 *
 * Tại sao cần smooth speed?
 * GPS raw speed nhảy lung tung: 0.5 → 3.2 → 0.8 → 2.1 m/s khi đi bộ đều.
 * Rolling average 5 samples làm ổn định: ~1.5 m/s liên tục.
 *
 * Sommerville §5.5: Đây là một Filter trong Pipe-and-Filter pattern.
 */
class SpeedSmoother(private val windowSize: Int = 5) {

    // Queue lưu N samples gần nhất
    private val samples = ArrayDeque<Float>(windowSize)

    /**
     * Thêm sample mới và trả về giá trị đã smooth.
     *
     * @param rawSpeed tốc độ GPS raw (m/s)
     * @return trung bình của windowSize samples gần nhất
     */
    fun add(rawSpeed: Float): Float {
        // Thêm sample mới
        samples.addLast(rawSpeed)

        // Nếu vượt quá windowSize, xóa sample cũ nhất
        if (samples.size > windowSize) {
            samples.removeFirst()
        }

        // Tính average
        return samples.average().toFloat()
    }

    /**
     * Reset về trạng thái ban đầu.
     * Gọi khi GPS bị dropout quá lâu hoặc chuyển mode đột ngột.
     */
    fun reset() {
        samples.clear()
    }

    /** Số samples hiện có (để debug) */
    fun sampleCount(): Int = samples.size
}