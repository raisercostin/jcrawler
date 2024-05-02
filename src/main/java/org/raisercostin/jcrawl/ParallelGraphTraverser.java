package org.raisercostin.jcrawl;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

  public ParallelGraphTraverser(int numberOfWorkerThreads, SuccessorsFunction<N> successorFunction) {
    this.numberOfWorkerThreads = numberOfWorkerThreads;
    this.successorFunction = successorFunction;
    this.visited = ConcurrentHashMap.newKeySet();
    this.horizonSet = ConcurrentHashMap.newKeySet();
    this.horizon = new LinkedBlockingQueue<>();
    this.visitedOrder = new LinkedBlockingQueue<>(); // Queue to track order of visited nodes
    this.executor = Executors.newFixedThreadPool(5);
  }

  private static class SentinelKillPill {}

  private static final SentinelKillPill STOP = new SentinelKillPill();

  public Iterable<N> startTraversal(N startNode) {
    horizon.add(startNode);
    for (int i = 0; i < numberOfWorkerThreads; i++) { // Start 5 worker threads
      executor.submit(() -> {
        try {
          while (!Thread.currentThread().isInterrupted()) {
            N current = horizon.take();
            if (current == STOP) {
              break; // Stop the thread
            }
            horizonSet.remove(current);
            //process current node
            if (visited.add(current)) {
              visitedOrder.add(current); // Add to order queue when visited
              Iterable<? extends N> successors = successorFunction.successors(current);
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
            }
          }
        } catch (InterruptedException e) {
          log.debug("Interrupted during shutdown.", e);
        }
      });
    }
    return topDown();
  }

  private void stopAll() {
    for (int i = 0; i < numberOfWorkerThreads; i++) {
      try {
        horizon.put((N) STOP);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Iterable<N> topDown() {
    return () -> new Iterator<>()
      {
        private N nextItem = null;

        @Override
        public boolean hasNext() {
          if (nextItem == null) {
            try {
              nextItem = visitedOrder.take(); // Blocks if no visited nodes are available
              if (nextItem == STOP) {
                nextItem = null;
              }
            } catch (InterruptedException e) {
              log.debug("Interrupted during shutdown.", e);
              Thread.currentThread().interrupt();
              shutdown();
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
    executor.shutdownNow();
    try {
      if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
        System.out.println("Executor did not terminate.");
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted during shutdown.", e);
    }
  }
}
