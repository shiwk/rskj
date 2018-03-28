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

/**
 * This follows Java Flow API, which was introduced in Java 9 and is not available at the moment.
 */
public final class Flow {

    private Flow() {
        // uninstantiable
    }

    public interface Publisher<T> {
        void subscribe(Subscriber<? super T> subscriber);
    }

    public interface Subscriber<T> {
        void onSubscribe(Subscription subscription);

        void onNext(T item);

        void onError(Throwable throwable);

        void onComplete();
    }

    public interface Subscription {
        void request(long n);

        void cancel();
    }

    public interface Processor<T,R> extends Subscriber<T>, Publisher<R> {
    }
}
