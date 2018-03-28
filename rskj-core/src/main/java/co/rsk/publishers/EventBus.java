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
package co.rsk.publishers;

import co.rsk.util.concurrent.BackpressureStrategy;
import co.rsk.util.concurrent.Flowable;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;

import java.util.List;

/**
 * This publishes RSK events.
 */
public class EventBus {
    private final Flowable<Block> newHeads;

    public EventBus(CompositeEthereumListener listener) {
        this.newHeads = Flowable.create(emitter -> {
            EthereumListenerAdapter al = new EthereumListenerAdapter() {
                @Override
                public void onBlock(Block block, List<TransactionReceipt> receipts) {
                    emitter.onNext(block);
                }
            };
            listener.addListener(al);
            // TODO listener.removeListener(al)
        }, BackpressureStrategy.BUFFER);
    }

    public Flowable<Block> newHeads() {
        return newHeads;
    }
}
