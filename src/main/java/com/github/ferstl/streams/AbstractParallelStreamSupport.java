package com.github.ferstl.streams;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.BaseStream;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ForkJoinTask.adapt;

abstract class AbstractParallelStreamSupport<T, S extends BaseStream<T, S>> implements BaseStream<T, S> {

  S delegate;
  final ForkJoinPool workerPool;

  AbstractParallelStreamSupport(S delegate, ForkJoinPool workerPool) {
    requireNonNull(delegate, "Stream must not be null");
    requireNonNull(workerPool, "Worker pool must not be null");

    this.delegate = delegate;
    this.workerPool = workerPool;
  }

  @Override
  public boolean isParallel() {
    return this.delegate.isParallel();
  }

  @Override
  public Iterator<T> iterator() {
    return this.delegate.iterator();
  }

  @Override
  public Spliterator<T> spliterator() {
    return this.delegate.spliterator();
  }

  @Override
  @SuppressWarnings("unchecked")
  public S sequential() {
    this.delegate = this.delegate.sequential();
    return (S) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public S parallel() {
    this.delegate = this.delegate.parallel();
    return (S) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public S unordered() {
    this.delegate = this.delegate.unordered();
    return (S) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public S onClose(Runnable closeHandler) {
    this.delegate = this.delegate.onClose(closeHandler);
    return (S) this;
  }

  @Override
  public void close() {
    this.delegate.close();
  }

  protected void execute(Runnable terminalOperation) {
    if (isParallel()) {
      ForkJoinTask<?> task = adapt(terminalOperation);
      this.workerPool.invoke(task);
    } else {
      terminalOperation.run();
    }
  }

  protected <R> R execute(Callable<R> terminalOperation) {
    if (isParallel()) {
      ForkJoinTask<R> task = adapt(terminalOperation);
      return this.workerPool.invoke(task);
    }

    try {
      return terminalOperation.call();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
