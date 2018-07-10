/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.consensus.poc.task;

import io.nuls.consensus.poc.cache.TxMemoryPool;
import io.nuls.consensus.poc.storage.service.TransactionCacheStorageService;
import io.nuls.consensus.poc.storage.service.TransactionQueueStorageService;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.constant.TransactionErrorCode;
import io.nuls.kernel.context.NulsContext;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.func.TimeService;
import io.nuls.kernel.model.Coin;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.model.Transaction;
import io.nuls.kernel.validate.ValidateResult;
import io.nuls.ledger.service.LedgerService;
import io.nuls.ledger.util.LedgerUtil;
import io.nuls.protocol.service.TransactionService;
import io.nuls.protocol.utils.TransactionTimeComparator;

import java.util.*;

/**
 * @author: Niels Wang
 * @date: 2018/7/5
 */
public class TxProcessTask implements Runnable {

    private TxMemoryPool pool = TxMemoryPool.getInstance();

    private LedgerService ledgerService = NulsContext.getServiceBean(LedgerService.class);
    private TransactionCacheStorageService transactionCacheStorageService = NulsContext.getServiceBean(TransactionCacheStorageService.class);
    private TransactionQueueStorageService transactionQueueStorageService = NulsContext.getServiceBean(TransactionQueueStorageService.class);

    private TransactionService transactionService = NulsContext.getServiceBean(TransactionService.class);

    private TransactionTimeComparator txComparator = TransactionTimeComparator.getInstance();

    private Map<String, Coin> temporaryToMap = new HashMap<>();
    private Set<String> temporaryFromSet = new HashSet<>();

    private List<Transaction> orphanTxList = new ArrayList<>();

    private static int maxOrphanSize = 200000;

//    int count = 0;
//    int size = 0;

    @Override
    public void run() {
        try {
            doTask();
        } catch (Exception e) {
            Log.error(e);
        }
        try {
            doOrphanTxTask();
        } catch (Exception e) {
            Log.error(e);
        }
//        System.out.println("count: " + count + " , size : " + size + " , orphan size : " + orphanTxList.size());
    }

    private void doTask() {

        if (TxMemoryPool.getInstance().getPoolSize() >= 1000000L) {
            return;
        }

        Transaction tx = null;
        while ((tx = transactionQueueStorageService.pollTx()) != null && orphanTxList.size() < maxOrphanSize) {
//            size++;
            processTx(tx, false);
        }
    }


    private void doOrphanTxTask() {
        orphanTxList.sort(txComparator);

        Iterator<Transaction> it = orphanTxList.iterator();
        while (it.hasNext()) {
            Transaction tx = it.next();
            boolean success = processTx(tx, true);
            if (success) {
                it.remove();
            }
        }
    }


    private boolean processTx(Transaction tx, boolean isOrphanTx) {
        try {
            Result result = tx.verify();
            if (result.isFailed()) {
                return false;
            }

            Transaction tempTx = ledgerService.getTx(tx.getHash());
            if (tempTx != null) {
                return isOrphanTx;
            }

            ValidateResult validateResult = ledgerService.verifyCoinData(tx, temporaryToMap, temporaryFromSet);
            if (validateResult.isSuccess()) {
                pool.add(tx, false);

                List<Coin> fromCoins = tx.getCoinData().getFrom();
                for (Coin coin : fromCoins) {
                    String key = LedgerUtil.asString(coin.getOwner());
                    temporaryFromSet.remove(key);
                    temporaryToMap.remove(key);
                }
//                count++;

                transactionCacheStorageService.putTx(tx);
                transactionService.forwardTx(tx, null);

                return true;
            } else if (validateResult.getErrorCode().equals(TransactionErrorCode.ORPHAN_TX) && !isOrphanTx) {
                processOrphanTx(tx);
            } else if (isOrphanTx) {
                return tx.getTime() < (TimeService.currentTimeMillis() - 3600000L);
            }
        } catch (Exception e) {
            Log.error(e);
        }
        return false;
    }

    private void processOrphanTx(Transaction tx) throws NulsException {
        orphanTxList.add(tx);
    }
}
