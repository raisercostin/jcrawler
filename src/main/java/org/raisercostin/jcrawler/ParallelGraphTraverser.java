package org.raisercostin.jcrawler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.graph.SuccessorsFunction;

public class ParallelGraphTraverser<N> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ParallelGraphTraverser.class);

  private SuccessorsFunction<N> successorFunction;
  private Set<N> visited;
  private BlockingQueue<N> horizon;
  private Set<N> horizonSet;
  private BlockingQueue<N> visitedOrder; // To maintain the order of visited nodes
  private ExecutorService executor;
  private int numberOfWorkerThreads;
  private AtomicInteger idleWorkers; // Track idle workers waiting on empty queue
  private volatile boolean terminated = false;

  public ParallelGraphTraverser(int numberOfWorkerThreads, SuccessorsFunction<N> successorFunction) {
    this.numberOfWorkerThreads = numberOfWorkerThreads;
    this.successorFunction = successorFunction;
    this.visited = ConcurrentHashMap.newKeySet();
    this.horizonSet = ConcurrentHashMap.newKeySet();
    this.horizon = new LinkedBlockingQueue<>();
    this.visitedOrder = new LinkedBlockingQueue<>(); // Queue to track order of visited nodes
    this.executor = Executors.newFixedThreadPool(numberOfWorkerThreads);
    this.idleWorkers = new AtomicInteger(0);
  }

  private static class SentinelKillPill {
  }

  private static final SentinelKillPill STOP = new SentinelKillPill();

  public Iterable<N> startTraversal(Iterable<N> startNodes, int maxDocs) {
    if (maxDocs == 0) {
      return new LinkedList<>();
    }
    AtomicInteger visitedCounter = new AtomicInteger(0);
    startNodes.forEach(horizon::add);
    for (int i = 0; i < numberOfWorkerThreads; i++) { // Start 5 worker threads
      int locali = i;
      executor.submit(() -> {
        try {
          while (!Thread.currentThread().isInterrupted() && !terminated) {
            // Use poll with timeout to allow checking for termination condition
            int idle = idleWorkers.incrementAndGet();
            N current = horizon.poll(500, TimeUnit.MILLISECONDS);

            if (current == null) {
              // Queue was empty - check if all workers are idle and queue is still empty
              // Check BEFORE decrementing so we can detect all workers waiting
              log.debug("Worker {} poll timeout: idle={}/{} horizon={} horizonSet={}",
                  locali, idle, numberOfWorkerThreads, horizon.size(), horizonSet.size());
              if (horizon.isEmpty() && horizonSet.isEmpty() && idle >= numberOfWorkerThreads) {
                // All workers idle and no more work - terminate
                log.debug("Worker {} detected termination condition: queue empty, idle={}/{}", locali, idle,
                    numberOfWorkerThreads);
                idleWorkers.decrementAndGet();
                terminateAll();
                break;
              }
              idleWorkers.decrementAndGet();
              continue; // Try again
            }
            idleWorkers.decrementAndGet();

            if (current == STOP) {
              break; // Stop the thread
            }
            horizonSet.remove(current);
            if (maxDocs <= 0 || visited.size() < maxDocs) {
              // process current node
              if (visited.add(current)) {
                log.debug("Worker {} processing item, visited={} consumed={} maxDocs={}", locali, visited.size(),
                    visitedCounter.get(), maxDocs);
                Iterable<? extends N> successors = null;
                try {
                  successors = successorFunction.successors(current);
                  log.debug("Worker {} got {} successors", locali, successors != null ? "some" : "null");
                } catch (Exception e) {
                  log.error("Worker {} exception getting successors: {}", locali, e.getMessage(), e);
                  successors = null;
                }
                if (successors != null) {
                  for (N successor : successors) {
                    if (!visited.contains(successor)) {
                      if (!horizonSet.contains(successor)) {
                        horizonSet.add(successor);
                        horizon.add(successor);
                      }
                    }
                  }
                }
                log.debug("Worker {} adding to visitedOrder", locali);
                visitedOrder.add(current); // Add to order queue when visited
                log.debug("Worker {} added to visitedOrder, incrementing counter", locali);
                if (maxDocs > 0 && visitedCounter.incrementAndGet() >= maxDocs) {
                  stopVisited();
                }
                if (maxDocs > 0 && visited.size() >= maxDocs) {
                  terminateAll();
                  break;
                }
              } else {
                log.debug("Worker {} skipped item (already visited)", locali);
              }
            } else {
              log.debug("Worker {} skipped item (maxDocs reached)", locali);
            }
          }
          if (maxDocs > 0 && visitedOrder.size() >= maxDocs) {
            stopVisited();
          }
        } catch (InterruptedException e) {
          log.debug("Interrupted during shutdown.", e);
        } finally {
          log.debug("Finished worker {} visited={} consumed={} maxDocs={}", locali, visited.size(),
              visitedCounter.get(), maxDocs);
        }
      });
    }
    return topDown();
  }

  private synchronized void terminateAll() {
    if (terminated)
      return;
    terminated = true;
    log.debug("Terminating all workers and shutting down executor");
    stopWorkers();
    stopVisited();
    // Shutdown executor to allow JVM to exit
    executor.shutdown();
  }

  private void stopWorkers() {
    for (int i = 0; i < numberOfWorkerThreads; i++) {
      try {
        horizon.put((N) STOP);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void stopVisited() {
    try {
      visitedOrder.put((N) STOP);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private Iterable<N> topDown() {
    return () -> new Iterator<>() {
      private N nextItem = null;
      private boolean receivedStop = false; // Track if we've received STOP

      @Override
      public boolean hasNext() {
        if (receivedStop) {
          return false; // Already ended, don't poll again
        }
        if (nextItem == null) {
          try {
            log.debug("Iterator waiting for next item... visitedOrder.size={} terminated={}", visitedOrder.size(),
                terminated);
            // Use poll with timeout instead of take to avoid infinite blocking
            while (!terminated && !receivedStop) {
              nextItem = visitedOrder.poll(500, TimeUnit.MILLISECONDS);
              if (nextItem != null) {
                if (nextItem == STOP) {
                  log.debug("Iterator received STOP sentinel");
                  receivedStop = true;
                  nextItem = null;
                  return false;
                }
                log.debug("Iterator got item: {}", nextItem);
                break;
              }
              log.debug("Iterator poll timeout, checking termination... terminated={}", terminated);
            }
            // If terminated flag is set, end iteration
            if (terminated) {
              log.debug("Iterator ending due to terminated flag");
              receivedStop = true;
              nextItem = null;
            }
          } catch (InterruptedException e) {
            log.debug("Interrupted during shutdown.", e);
            Thread.currentThread().interrupt();
            return false;
          }
        }
        return nextItem != null;
      }

      @Override
      public N next() {
        if (nextItem == null && !hasNext()) {
          throw new NoSuchElementException();
        }
        N item = nextItem;
        nextItem = null;
        return item;
      }
    };
  }

  public void shutdown() {
    stopWorkers();
    stopVisited();
    executor.shutdownNow();
    try {
      if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
        log.warn("Executor did not terminate.");
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted during shutdown.", e);
    }
  }
}
