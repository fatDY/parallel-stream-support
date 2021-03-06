/*
 * Copyright (c) 2016 Stefan Ferstl <st.ferstl@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.ferstl.streams;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStream.Builder;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class ParallelDoubleStreamSupportTest extends AbstractParallelStreamSupportTest<Double, DoubleStream, ParallelDoubleStreamSupport> {

  private Stream<?> mappedDelegateMock;
  private IntStream mappedIntDelegateMock;
  private LongStream mappedLongDelegateMock;
  private DoubleStream mappedDoubleDelegateMock;
  private PrimitiveIterator.OfDouble iteratorMock;
  private Spliterator.OfDouble spliteratorMock;
  private double[] toArrayResult;
  private DoubleSummaryStatistics summaryStatistics;

  private ParallelDoubleStreamSupport parallelDoubleStreamSupport;

  @Override
  protected ParallelDoubleStreamSupport createParallelStreamSupportMock(ForkJoinPool workerPool) {
    return new ParallelDoubleStreamSupport(mock(DoubleStream.class), workerPool);
  }

  @BeforeEach
  @SuppressWarnings({"unchecked", "rawtypes"})
  void init() {
    // Precondition for all tests
    assertFalse(currentThread() instanceof ForkJoinWorkerThread, "This test must not run in a ForkJoinPool");

    this.mappedDelegateMock = mock(Stream.class);
    this.mappedIntDelegateMock = mock(IntStream.class);
    this.mappedLongDelegateMock = mock(LongStream.class);
    this.mappedDoubleDelegateMock = mock(DoubleStream.class);
    this.iteratorMock = mock(PrimitiveIterator.OfDouble.class);
    this.spliteratorMock = mock(Spliterator.OfDouble.class);
    this.toArrayResult = new double[0];
    this.summaryStatistics = new DoubleSummaryStatistics();

    when(this.delegateMock.map(any())).thenReturn(this.mappedDoubleDelegateMock);
    when(this.delegateMock.mapToObj(any())).thenReturn((Stream) this.mappedDelegateMock);
    when(this.delegateMock.mapToInt(any())).thenReturn(this.mappedIntDelegateMock);
    when(this.delegateMock.mapToLong(any())).thenReturn(this.mappedLongDelegateMock);
    when(this.delegateMock.flatMap(any())).thenReturn(this.mappedDoubleDelegateMock);
    when(this.delegateMock.iterator()).thenReturn(this.iteratorMock);
    when(this.delegateMock.spliterator()).thenReturn(this.spliteratorMock);
    when(this.delegateMock.isParallel()).thenReturn(false);
    when(this.delegateMock.toArray()).thenReturn(this.toArrayResult);
    when(this.delegateMock.reduce(anyDouble(), any())).thenReturn(42.0);
    when(this.delegateMock.reduce(any())).thenReturn(OptionalDouble.of(42.0));
    when(this.delegateMock.collect(any(), any(), any())).thenReturn("collect");
    when(this.delegateMock.sum()).thenReturn(42.0);
    when(this.delegateMock.min()).thenReturn(OptionalDouble.of(42.0));
    when(this.delegateMock.max()).thenReturn(OptionalDouble.of(42.0));
    when(this.delegateMock.count()).thenReturn(42L);
    when(this.delegateMock.average()).thenReturn(OptionalDouble.of(42.0));
    when(this.delegateMock.summaryStatistics()).thenReturn(this.summaryStatistics);
    when(this.delegateMock.anyMatch(any())).thenReturn(true);
    when(this.delegateMock.allMatch(any())).thenReturn(true);
    when(this.delegateMock.noneMatch(any())).thenReturn(true);
    when(this.delegateMock.findFirst()).thenReturn(OptionalDouble.of(42.0));
    when(this.delegateMock.findAny()).thenReturn(OptionalDouble.of(42.0));
    when(this.delegateMock.boxed()).thenReturn((Stream) this.mappedDelegateMock);

    DoubleStream delegate = DoubleStream.of(1.0).parallel();
    this.parallelDoubleStreamSupport = new ParallelDoubleStreamSupport(delegate, this.workerPool);
  }


  @Test
  void parallelStreamWithArray() {
    DoubleStream stream = ParallelDoubleStreamSupport.parallelStream(new double[]{42.0}, this.workerPool);

    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertTrue(stream.isParallel());
    assertEquals(OptionalDouble.of(42.0), stream.findAny());
  }

  @Test
  void parallelStreamWithNullArray() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.parallelStream((double[]) null, this.workerPool));
  }

  @Test
  void parallelStreamSupportWithSpliterator() {
    Spliterator.OfDouble spliterator = DoubleStream.of(42.0).spliterator();
    DoubleStream stream = ParallelDoubleStreamSupport.parallelStream(spliterator, this.workerPool);

    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertTrue(stream.isParallel());
    assertEquals(OptionalDouble.of(42.0), stream.findAny());
  }

  @Test
  void parallelStreamSupportWithNullSpliterator() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.parallelStream((Spliterator.OfDouble) null, this.workerPool));
  }

  @Test
  void parallelStreamSupportWithSpliteratorSupplier() {
    Supplier<Spliterator.OfDouble> supplier = () -> DoubleStream.of(42.0).spliterator();
    DoubleStream stream = ParallelDoubleStreamSupport.parallelStream(supplier, 0, this.workerPool);

    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertTrue(stream.isParallel());
    assertEquals(OptionalDouble.of(42.0), stream.findAny());
  }

  @Test
  void parallelStreamSupportWithNullSpliteratorSupplier() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.parallelStream(null, 0, this.workerPool));
  }

  @Test
  void parallelStreamWithBuilder() {
    Builder builder = DoubleStream.builder();
    builder.accept(42.0);
    DoubleStream stream = ParallelDoubleStreamSupport.parallelStream(builder, this.workerPool);

    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertTrue(stream.isParallel());
    assertEquals(OptionalDouble.of(42.0), stream.findAny());
  }

  @Test
  void parallelStreamWithNullBuilder() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.parallelStream((Builder) null, this.workerPool));
  }

  @Test
  void iterate() {
    DoubleUnaryOperator operator = a -> a;
    DoubleStream stream = ParallelDoubleStreamSupport.iterate(42.0, operator, this.workerPool);

    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertTrue(stream.isParallel());
    assertEquals(OptionalDouble.of(42.0), stream.findAny());
  }

  @Test
  void iterateWithNullOperator() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.iterate(42.0, null, this.workerPool));
  }

  @Test
  void generate() {
    DoubleSupplier supplier = () -> 42.0;
    DoubleStream stream = ParallelDoubleStreamSupport.generate(supplier, this.workerPool);

    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertTrue(stream.isParallel());
    assertEquals(OptionalDouble.of(42.0), stream.findAny());
  }

  @Test
  void generateWithNullSupplier() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.generate(null, this.workerPool));
  }

  @Test
  void concat() {
    DoubleStream a = DoubleStream.of(42.0);
    DoubleStream b = DoubleStream.of(43);
    DoubleStream stream = ParallelDoubleStreamSupport.concat(a, b, this.workerPool);

    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertTrue(stream.isParallel());
    assertArrayEquals(stream.toArray(), new double[]{42.0, 43.0}, 0.000001);
  }

  @Test
  void concatWithNullStreamA() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.concat(null, DoubleStream.of(42.0), this.workerPool));
  }

  @Test
  void concatWithNullStreamB() {
    assertThrows(NullPointerException.class, () -> ParallelDoubleStreamSupport.concat(DoubleStream.of(42.0), null, this.workerPool));
  }

  @Test
  void filter() {
    DoublePredicate p = d -> true;
    DoubleStream stream = this.parallelStreamSupportMock.filter(p);

    verify(this.delegateMock).filter(p);
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void map() {
    DoubleUnaryOperator f = d -> 42;
    DoubleStream stream = this.parallelStreamSupportMock.map(f);

    verify(this.delegateMock).map(f);
    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertSame(((ParallelDoubleStreamSupport) stream).delegate, this.mappedDoubleDelegateMock);
    assertSame(((ParallelDoubleStreamSupport) stream).workerPool, this.workerPool);
  }

  @Test
  void mapToObj() {
    DoubleFunction<String> f = d -> "x";
    Stream<String> stream = this.parallelStreamSupportMock.mapToObj(f);

    verify(this.delegateMock).mapToObj(f);
    assertThat(stream, instanceOf(ParallelStreamSupport.class));
    assertSame(((ParallelStreamSupport) stream).delegate, this.mappedDelegateMock);
    assertSame(((ParallelStreamSupport) stream).workerPool, this.workerPool);
  }

  @Test
  void mapToInt() {
    DoubleToIntFunction f = d -> 1;
    IntStream stream = this.parallelStreamSupportMock.mapToInt(f);

    verify(this.delegateMock).mapToInt(f);
    assertThat(stream, instanceOf(ParallelIntStreamSupport.class));
    assertSame(((ParallelIntStreamSupport) stream).delegate, this.mappedIntDelegateMock);
    assertSame(((ParallelIntStreamSupport) stream).workerPool, this.workerPool);
  }

  @Test
  void mapToLong() {
    DoubleToLongFunction f = d -> 1L;
    LongStream stream = this.parallelStreamSupportMock.mapToLong(f);

    verify(this.delegateMock).mapToLong(f);
    assertThat(stream, instanceOf(ParallelLongStreamSupport.class));
    assertSame(((ParallelLongStreamSupport) stream).delegate, this.mappedLongDelegateMock);
    assertSame(((ParallelLongStreamSupport) stream).workerPool, this.workerPool);
  }

  @Test
  void flatMap() {
    DoubleFunction<DoubleStream> f = d -> DoubleStream.of(1.0);
    DoubleStream stream = this.parallelStreamSupportMock.flatMap(f);

    verify(this.delegateMock).flatMap(f);
    assertThat(stream, instanceOf(ParallelDoubleStreamSupport.class));
    assertSame(((ParallelDoubleStreamSupport) stream).delegate, this.mappedDoubleDelegateMock);
  }

  @Test
  void distinct() {
    DoubleStream stream = this.parallelStreamSupportMock.distinct();

    verify(this.delegateMock).distinct();
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void sorted() {
    DoubleStream stream = this.parallelStreamSupportMock.sorted();

    verify(this.delegateMock).sorted();
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void peek() {
    DoubleConsumer c = d -> {
    };
    DoubleStream stream = this.parallelStreamSupportMock.peek(c);

    verify(this.delegateMock).peek(c);
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void limit() {
    DoubleStream stream = this.parallelStreamSupportMock.limit(5);

    verify(this.delegateMock).limit(5);
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void skip() {
    DoubleStream stream = this.parallelStreamSupportMock.skip(5);

    verify(this.delegateMock).skip(5);
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void takeWhile() {
    DoublePredicate predicate = x -> true;
    DoubleStream stream = this.parallelStreamSupportMock.takeWhile(predicate);

    verify(this.delegateMock).takeWhile(predicate);
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void dropWhile() {
    DoublePredicate predicate = x -> true;
    DoubleStream stream = this.parallelStreamSupportMock.dropWhile(predicate);

    verify(this.delegateMock).dropWhile(predicate);
    assertSame(this.parallelStreamSupportMock, stream);
  }

  @Test
  void forEach() {
    DoubleConsumer c = d -> {
    };
    this.parallelStreamSupportMock.forEach(c);

    verify(this.delegateMock).forEach(c);
  }

  @Test
  void forEachSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport.forEach(d -> threadRef.set(currentThread()));

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void forEachParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport.forEach(d -> threadRef.set(currentThread()));

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void forEachOrdered() {
    DoubleConsumer c = d -> {
    };
    this.parallelStreamSupportMock.forEachOrdered(c);

    verify(this.delegateMock).forEachOrdered(c);
  }

  @Test
  void forEachOrderedSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport.forEachOrdered(d -> threadRef.set(currentThread()));

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void forEachOrderedParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport.forEachOrdered(d -> threadRef.set(currentThread()));

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void toArray() {
    double[] array = this.parallelStreamSupportMock.toArray();

    verify(this.delegateMock).toArray();
    assertSame(this.toArrayResult, array);
  }

  @Test
  void toArraySequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .toArray();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void toArrayParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .toArray();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void reduceWithIdentityAndAccumulator() {
    DoubleBinaryOperator accumulator = (a, b) -> b;
    double result = this.parallelStreamSupportMock.reduce(0, accumulator);

    verify(this.delegateMock).reduce(0, accumulator);
    assertEquals(42.0, result, 0.000001);
  }

  @Test
  void reduceWithIdentityAndAccumulatorSequential() {
    this.parallelDoubleStreamSupport.sequential();
    DoubleBinaryOperator accumulator = (a, b) -> b;
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .reduce(0, accumulator);

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void reduceWithIdentityAndAccumulatorParallel() {
    this.parallelDoubleStreamSupport.parallel();
    DoubleBinaryOperator accumulator = (a, b) -> b;
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .reduce(0, accumulator);

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void reduceWithAccumulator() {
    DoubleBinaryOperator accumulator = (a, b) -> b;
    OptionalDouble result = this.parallelStreamSupportMock.reduce(accumulator);

    verify(this.delegateMock).reduce(accumulator);
    assertEquals(OptionalDouble.of(42), result);
  }

  @Test
  void reduceWithAccumulatorSequential() {
    this.parallelDoubleStreamSupport.sequential();
    DoubleBinaryOperator accumulator = (a, b) -> b;
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .reduce(accumulator);

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void reduceWithAccumulatorParallel() {
    this.parallelDoubleStreamSupport.parallel();
    DoubleBinaryOperator accumulator = (a, b) -> b;
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .reduce(accumulator);

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void collectWithSupplierAndAccumulatorAndCombiner() {
    Supplier<String> supplier = () -> "x";
    ObjDoubleConsumer<String> accumulator = (a, b) -> {
    };
    BiConsumer<String, String> combiner = (a, b) -> {
    };

    String result = this.parallelStreamSupportMock.collect(supplier, accumulator, combiner);

    verify(this.delegateMock).collect(supplier, accumulator, combiner);
    assertEquals("collect", result);
  }

  @Test
  void collectWithSupplierAndAccumulatorAndCombinerSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void collectWithSupplierAndAccumulatorAndCombinerParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void sum() {
    double result = this.parallelStreamSupportMock.sum();

    verify(this.delegateMock).sum();
    assertEquals(42.0, result, 0.000001);
  }

  @Test
  void sumSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .sum();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void sumParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .sum();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void min() {
    OptionalDouble result = this.parallelStreamSupportMock.min();

    verify(this.delegateMock).min();
    assertEquals(OptionalDouble.of(42), result);
  }

  @Test
  void minSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .min();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void minParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .min();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void max() {
    OptionalDouble result = this.parallelStreamSupportMock.max();

    verify(this.delegateMock).max();
    assertEquals(OptionalDouble.of(42), result);
  }

  @Test
  void maxSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .max();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void maxParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .max();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void count() {
    long count = this.parallelStreamSupportMock.count();

    verify(this.delegateMock).count();
    assertEquals(42L, count);
  }

  @Test
  void countSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .filter(d -> {
          // Don't use peek() in combination with count(). See Javadoc.
          threadRef.set(currentThread());
          return true;
        }).count();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void countParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .filter(d -> {
          // Don't use peek() in combination with count(). See Javadoc.
          threadRef.set(currentThread());
          return true;
        }).count();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void average() {
    OptionalDouble result = this.parallelStreamSupportMock.average();

    verify(this.delegateMock).average();
    assertEquals(OptionalDouble.of(42.0), result);
  }

  @Test
  void averageSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .average();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void averageParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .average();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void summaryStatistics() {
    DoubleSummaryStatistics result = this.parallelStreamSupportMock.summaryStatistics();

    verify(this.delegateMock).summaryStatistics();
    assertEquals(this.summaryStatistics, result);
  }

  @Test
  void summaryStatisticsSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .summaryStatistics();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void summaryStatisticsParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .summaryStatistics();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void anyMatch() {
    DoublePredicate p = d -> true;

    boolean result = this.parallelStreamSupportMock.anyMatch(p);

    verify(this.delegateMock).anyMatch(p);
    assertTrue(result);
  }

  @Test
  void anyMatchSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .anyMatch(d -> true);

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void anyMatchParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .anyMatch(d -> true);

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void allMatch() {
    DoublePredicate p = d -> true;

    boolean result = this.parallelStreamSupportMock.allMatch(p);

    verify(this.delegateMock).allMatch(p);
    assertTrue(result);
  }

  @Test
  void allMatchSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .allMatch(d -> true);

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void allMatchParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .allMatch(d -> true);

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void noneMatch() {
    DoublePredicate p = d -> true;

    boolean result = this.parallelStreamSupportMock.noneMatch(p);

    verify(this.delegateMock).noneMatch(p);
    assertTrue(result);
  }

  @Test
  void noneMatchSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .noneMatch(d -> true);

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void noneMatchParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .noneMatch(d -> true);

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void findFirst() {
    OptionalDouble result = this.parallelStreamSupportMock.findFirst();

    verify(this.delegateMock).findFirst();
    assertEquals(OptionalDouble.of(42), result);
  }

  @Test
  void findFirstSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .findFirst();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void findFirstParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .findFirst();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void findAny() {
    OptionalDouble result = this.parallelStreamSupportMock.findAny();

    verify(this.delegateMock).findAny();
    assertEquals(OptionalDouble.of(42), result);
  }

  @Test
  void findAnytSequential() {
    this.parallelDoubleStreamSupport.sequential();
    Thread thisThread = currentThread();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .findAny();

    assertEquals(thisThread, threadRef.get());
  }

  @Test
  void findAnyParallel() {
    this.parallelDoubleStreamSupport.parallel();
    AtomicReference<Thread> threadRef = new AtomicReference<>();

    this.parallelDoubleStreamSupport
        .peek(d -> threadRef.set(currentThread()))
        .findAny();

    assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
  }

  @Test
  void boxed() {
    Stream<Double> stream = this.parallelStreamSupportMock.boxed();

    verify(this.delegateMock).boxed();
    assertThat(stream, instanceOf(ParallelStreamSupport.class));
    assertSame(this.mappedDelegateMock, ParallelStreamSupport.class.cast(stream).delegate);
    assertSame(this.workerPool, ParallelStreamSupport.class.cast(stream).workerPool);
  }

  @Override
  @Test
  void iterator() {
    PrimitiveIterator.OfDouble iterator = this.parallelStreamSupportMock.iterator();

    verify(this.delegateMock).iterator();
    assertSame(this.iteratorMock, iterator);
  }

  @Override
  @Test
  void spliterator() {
    Spliterator.OfDouble spliterator = this.parallelStreamSupportMock.spliterator();

    verify(this.delegateMock).spliterator();
    assertSame(this.spliteratorMock, spliterator);
  }
}
