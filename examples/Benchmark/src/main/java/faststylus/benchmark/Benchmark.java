package faststylus.benchmark;

import faststylus.FastStylus;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Benchmark {

    private FastStylus.StylusEvent sampleEvent;

    @Setup
    public void setup() {
        sampleEvent = new FastStylus.StylusEvent(
            1, 200, 300, 512, 15, -10, 45, 
            10, 10, System.currentTimeMillis(), 
            FastStylus.State.MOVE, false, false, false, false
        );
    }

    @org.openjdk.jmh.annotations.Benchmark
    public FastStylus.StylusEvent benchmarkStylusEventAllocation() {
        return new FastStylus.StylusEvent(
            1, 200, 300, 512, 15, -10, 45, 
            10, 10, System.currentTimeMillis(), 
            FastStylus.State.MOVE, false, false, false, false
        );
    }

    @org.openjdk.jmh.annotations.Benchmark
    public String benchmarkStylusEventFormatting() {
        return sampleEvent.toString();
    }
}
