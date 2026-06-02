package simulation;

import gamemechanics.Event;
import gamemechanics.Match;
import java.util.ArrayList;
import java.util.function.Consumer;

public class LiveMatchRunner implements Runnable {
    private final Match match;
    private final SimulationEngine engine;
    private final int minuteStep;
    private final long delayMillis;
    private final Consumer<ArrayList<Event>> updateHandler;
    private volatile boolean running;
    private volatile boolean paused;

    // public LiveMatchRunner(Match match, SimulationEngine engine) {
    //     this(match, engine, 1, 750, null);
    // }

    public LiveMatchRunner(Match match, SimulationEngine engine, int minuteStep, long delayMillis, Consumer<ArrayList<Event>> updateHandler) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");
        if (engine == null) throw new IllegalArgumentException("Simulation engine cannot be null.");
        if (minuteStep <= 0) throw new IllegalArgumentException("Minute step must be greater than 0.");
        if (delayMillis < 0) throw new IllegalArgumentException("Delay cannot be negative.");

        this.match = match;
        this.engine = engine;
        this.minuteStep = minuteStep;
        this.delayMillis = delayMillis;
        this.updateHandler = updateHandler;
        this.running = false;
        this.paused = false;
    }

    @Override
    public void run() {
        this.running = true;

        try {
            while (this.running && !match.isFinished()) {
                waitIfPaused();

                if (!this.running || match.isFinished()) break;

                ArrayList<Event> newEvents = engine.advanceMatch(match, minuteStep);

                if (this.updateHandler != null && !newEvents.isEmpty()) {
                    this.updateHandler.accept(newEvents);
                }

                Thread.sleep(this.delayMillis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.running = false;
        }
    }

    public void stop() {
        this.running = false;
        this.paused = false;
    }

    public void pause() {
        if (this.running) {
            this.paused = true;
        }
    }

    public void resume() {
        this.paused = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isPaused() {
        return this.paused;
    }

    private void waitIfPaused() throws InterruptedException {
        while (this.running && this.paused) {
            Thread.sleep(100);
        }
    }
}
