/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk.util.concurrent;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class FlowableCreate<T> extends Flowable<T> {
    private final FlowableOnSubscribe<T> source;
    private final BackpressureStrategy backpressure;

    public FlowableCreate(FlowableOnSubscribe<T> source, BackpressureStrategy backpressure) {
        this.source = source;
        this.backpressure = backpressure;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> t) {
        BufferEmitter<T> emitter;

        switch (backpressure) {
            default: { // case BUFFER
                emitter = new BufferEmitter<>(t);
                break;
            }
        }

        t.onSubscribe(emitter);
        source.subscribe(emitter);
    }

    /**
     * This implementation buffers all non-consumed values in a queue, which can grow unbounded.
     * It is one of the simplest backpressure mechanisms, and it is what RSKj currently does in most cases.
     */
    private static class BufferEmitter<T> implements FlowableEmitter<T>, Flow.Subscription {
        private final AtomicBoolean draining;
        private final AtomicLong requested;
        // TODO review queue type
        private final LinkedBlockingQueue<T> queue;
        private final Flow.Subscriber<? super T> actual;

        private BufferEmitter(Flow.Subscriber<? super T> actual) {
            this.draining = new AtomicBoolean();
            this.requested = new AtomicLong();
            this.queue = new LinkedBlockingQueue<>();
            this.actual = actual;
        }

        @Override
        public void request(long n) {
            this.requested.addAndGet(n);
            drain();
        }

        @Override
        public void cancel() {
            // TODO
        }

        @Override
        public void onNext(T item) {
            Objects.requireNonNull(item);
            queue.offer(item);
            drain();
        }

        private void drain() {
            // We can return immediately if we're already draining or there's no requested items
            if (draining.getAndSet(true) || requested.get() == 0) {
                return;
            }

            while (requested.decrementAndGet() > 0) {
                T o = queue.poll();
                if (o == null) {
                    break;
                }

                actual.onNext(o);
            }

            draining.getAndSet(false);
        }
    }
}
