/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2018 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */
package io.nuls.consensus.poc.process;

import io.nuls.consensus.poc.cache.TxMemoryPool;
import io.nuls.consensus.poc.config.ConsensusConfig;
import io.nuls.consensus.poc.constant.BlockContainerStatus;
import io.nuls.consensus.poc.constant.ConsensusStatus;
import io.nuls.consensus.poc.constant.PocConsensusConstant;
import io.nuls.consensus.poc.container.BlockContainer;
import io.nuls.consensus.poc.context.ConsensusStatusContext;
import io.nuls.consensus.poc.manager.ChainManager;
import io.nuls.consensus.poc.model.BlockData;
import io.nuls.consensus.poc.model.BlockRoundData;
import io.nuls.consensus.poc.model.MeetingMember;
import io.nuls.consensus.poc.model.MeetingRound;
import io.nuls.consensus.poc.protocol.constant.PunishReasonEnum;
import io.nuls.consensus.poc.protocol.entity.RedPunishData;
import io.nuls.consensus.poc.protocol.tx.RedPunishTransaction;
import io.nuls.consensus.poc.protocol.tx.YellowPunishTransaction;
import io.nuls.consensus.poc.provider.BlockQueueProvider;
import io.nuls.consensus.poc.util.ConsensusTool;
import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.dto.ContractTransfer;
import io.nuls.contract.entity.tx.CallContractTransaction;
import io.nuls.contract.entity.tx.ContractTransaction;
import io.nuls.contract.entity.tx.ContractTransferTransaction;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.contract.service.ContractService;
import io.nuls.core.tools.calc.LongUtils;
import io.nuls.core.tools.crypto.Hex;
import io.nuls.core.tools.date.DateUtil;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.constant.TransactionErrorCode;
import io.nuls.kernel.context.NulsContext;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.func.TimeService;
import io.nuls.kernel.model.*;
import io.nuls.kernel.validate.ValidateResult;
import io.nuls.ledger.service.LedgerService;
import io.nuls.network.service.NetworkService;
import io.nuls.protocol.cache.TemporaryCacheManager;
import io.nuls.protocol.constant.ProtocolConstant;
import io.nuls.protocol.model.SmallBlock;
import io.nuls.protocol.model.tx.CoinBaseTransaction;
import io.nuls.protocol.service.BlockService;
import io.nuls.protocol.service.TransactionService;
import io.nuls.protocol.utils.SmallBlockDuplicateRemoval;

import java.io.IOException;
import java.util.*;

/**
 * @author ln
 */
public class ConsensusProcess {

    private ChainManager chainManager;

    private TxMemoryPool txMemoryPool = TxMemoryPool.getInstance();
    private BlockQueueProvider blockQueueProvider = BlockQueueProvider.getInstance();

    private NetworkService networkService = NulsContext.getServiceBean(NetworkService.class);
    private LedgerService ledgerService = NulsContext.getServiceBean(LedgerService.class);
    private BlockService blockService = NulsContext.getServiceBean(BlockService.class);

    private TransactionService transactionService = NulsContext.getServiceBean(TransactionService.class);
    private ContractService contractService = NulsContext.getServiceBean(ContractService.class);

    private TemporaryCacheManager temporaryCacheManager = TemporaryCacheManager.getInstance();

    private boolean hasPacking;
    private long memoryPoolLastClearTime;

    public ConsensusProcess(ChainManager chainManager) {
        this.chainManager = chainManager;
    }

    public void process() {
        boolean canPackage = checkCanPackage();
        if (!canPackage) {
            return;
        }
        doWork();
    }

    private void doWork() {
        if (ConsensusStatusContext.getConsensusStatus().ordinal() < ConsensusStatus.RUNNING.ordinal()) {
            return;
        }
        MeetingRound round = chainManager.getMasterChain().getOrResetCurrentRound();
        if (round == null) {
            return;
        }
        //check i am is a consensus node
        MeetingMember member = round.getMyMember();
        if (member == null) {
            clearTxMemoryPool();
            return;
        }
        if (!hasPacking && member.getPackStartTime() < TimeService.currentTimeMillis() && member.getPackEndTime() > TimeService.currentTimeMillis()) {
            hasPacking = true;
            try {
                if (Log.isDebugEnabled()) {
                    Log.debug("当前网络时间： " + DateUtil.convertDate(new Date(TimeService.currentTimeMillis())) + " , 我的打包开始时间: " +
                            DateUtil.convertDate(new Date(member.getPackStartTime())) + " , 我的打包结束时间: " +
                            DateUtil.convertDate(new Date(member.getPackEndTime())) + " , 当前轮开始时间: " +
                            DateUtil.convertDate(new Date(round.getStartTime())) + " , 当前轮结束开始时间: " +
                            DateUtil.convertDate(new Date(round.getEndTime())));
                }
                packing(member, round);
            } catch (Exception e) {
                Log.error(e);
            }

            while (member.getPackEndTime() > TimeService.currentTimeMillis()) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    Log.error(e);
                }
            }
            hasPacking = false;
        }
    }

    private void packing(MeetingMember self, MeetingRound round) throws IOException, NulsException {
        Log.debug(round.toString());
        Log.info("packing 入口==========================");

        boolean needCheckAgain = waitReceiveNewestBlock(self, round);
        long start = System.currentTimeMillis();
        Block block = doPacking(self, round);
        Log.info("doPacking use:" + (System.currentTimeMillis() - start) + "ms");
        if (needCheckAgain && hasReceiveNewestBlock(self, round)) {
            Block realBestBlock = chainManager.getBestBlock();
            if (null != realBestBlock) {
                List<NulsDigestData> txHashList = realBestBlock.getTxHashList();
                for (Transaction transaction : block.getTxs()) {
                    if (transaction.isSystemTx()) {
                        continue;
                    }
                    if (txHashList.contains(transaction.getHash())) {
                        continue;
                    }
                    txMemoryPool.add(transaction, false);
                }
                start = System.currentTimeMillis();
                block = doPacking(self, round);
                Log.info("doPacking2 use:" + (System.currentTimeMillis() - start) + "ms");
            }
        }
        if (null == block) {
            Log.error("make a null block");
            return;
        }

        boolean success = saveBlock(block);
        if (success) {
            broadcastSmallBlock(block);
        } else {
            Log.error("make a block, but save block error");
        }
    }

    private void clearTxMemoryPool() {
        if (TimeService.currentTimeMillis() - memoryPoolLastClearTime > 60000L) {
            txMemoryPool.clear();
            memoryPoolLastClearTime = TimeService.currentTimeMillis();
        }
    }

    private boolean checkCanPackage() {
        if (!ConsensusConfig.isPartakePacking()) {
            this.clearTxMemoryPool();
            return false;
        }
        // wait consensus ready running
        if (ConsensusStatusContext.getConsensusStatus().ordinal() <= ConsensusStatus.WAIT_RUNNING.ordinal()) {
            return false;
        }
        // check network status
        if (networkService.getAvailableNodes().size() < ProtocolConstant.ALIVE_MIN_NODE_COUNT) {
            return false;
        }
        return true;
    }

    private boolean waitReceiveNewestBlock(MeetingMember self, MeetingRound round) {

        long timeout = ProtocolConstant.BLOCK_TIME_INTERVAL_MILLIS / 2;
        long endTime = self.getPackStartTime() + timeout;

        boolean hasReceiveNewestBlock = false;

        try {
            int i=0;
            while (!hasReceiveNewestBlock) {
                Log.info("=========================================waitReceiveNewestBlock: " + ++i);
                hasReceiveNewestBlock = hasReceiveNewestBlock(self, round);
                if (hasReceiveNewestBlock) {
                    break;
                }
                Thread.sleep(100L);
                if (TimeService.currentTimeMillis() >= endTime) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Log.error(e);
        }

        return !hasReceiveNewestBlock;
    }

    private boolean hasReceiveNewestBlock(MeetingMember self, MeetingRound round) {
        BlockHeader bestBlockHeader = blockService.getBestBlockHeader().getData();
        byte[] packingAddress = bestBlockHeader.getPackingAddress();

        int thisIndex = self.getPackingIndexOfRound();

        MeetingMember preMember;
        if (thisIndex == 1) {
            MeetingRound preRound = round.getPreRound();
            if (preRound == null) {
                Log.error("PreRound is null!");
                return true;
            }
            preMember = preRound.getMember(preRound.getMemberCount());
        } else {
            preMember = round.getMember(self.getPackingIndexOfRound() - 1);
        }
        if (preMember == null) {
            return true;
        }
        byte[] preBlockPackingAddress = preMember.getPackingAddress();
        long thisRoundIndex = preMember.getRoundIndex();
        int thisPackageIndex = preMember.getPackingIndexOfRound();

        BlockRoundData blockRoundData = new BlockRoundData(bestBlockHeader.getExtend());
        long roundIndex = blockRoundData.getRoundIndex();
        int packageIndex = blockRoundData.getPackingIndexOfRound();

        if (Arrays.equals(packingAddress, preBlockPackingAddress) && thisRoundIndex == roundIndex && thisPackageIndex == packageIndex) {
            return true;
        } else {
            return false;
        }
    }

    private boolean saveBlock(Block block) throws IOException {
        return blockQueueProvider.put(new BlockContainer(block, BlockContainerStatus.RECEIVED));
    }

    private void broadcastSmallBlock(Block block) {
        SmallBlock smallBlock = ConsensusTool.getSmallBlock(block);
        temporaryCacheManager.cacheSmallBlock(smallBlock);
        SmallBlockDuplicateRemoval.needDownloadSmallBlock(smallBlock.getHeader().getHash());
        blockService.broadcastBlock(smallBlock);
    }

    private Block doPacking(MeetingMember self, MeetingRound round) throws NulsException, IOException {

        Block bestBlock = chainManager.getBestBlock();

        BlockData bd = new BlockData();
        bd.setHeight(bestBlock.getHeader().getHeight() + 1);
        bd.setPreHash(bestBlock.getHeader().getHash());
        bd.setTime(self.getPackEndTime());
        BlockRoundData roundData = new BlockRoundData();
        roundData.setRoundIndex(round.getIndex());
        roundData.setConsensusMemberCount(round.getMemberCount());
        roundData.setPackingIndexOfRound(self.getPackingIndexOfRound());
        roundData.setRoundStartTime(round.getStartTime());

        StringBuilder str = new StringBuilder();
        str.append(self.getPackingAddress());
        str.append(" ,order:" + self.getPackingIndexOfRound());
        str.append(",packTime:" + new Date(self.getPackEndTime()));
        str.append("\n");
        Log.debug("pack round:" + str);

        bd.setRoundData(roundData);

        List<Transaction> packingTxList = new ArrayList<>();
        Set<NulsDigestData> outHashSet = new HashSet<>();

        long totalSize = 0L;

        Map<String, Coin> toMaps = new HashMap<>();
        Set<String> fromSet = new HashSet<>();

        long t1 = 0, t2 = 0;

        long time = System.currentTimeMillis();

        /**
         * pierre add 智能合约相关
         */
        byte[] stateRoot = bestBlock.getHeader().getStateRoot();
        // 更新世界状态根
        bd.setStateRoot(stateRoot);
        long height = bestBlock.getHeader().getHeight();
        Result<ContractResult> callContractResult = null;
        ContractResult contractResult = null;
        List<ContractTransfer> transfers = null;
        List<String> contractEvents = null;
        Map<String, ContractTransferTransaction> successContractTransferTxs = null;
        boolean isCorrectContractTransfer = true;
        long contractTransferTxTotalSize = 0L;
        Map<String, Coin> contractUsedCoinMap = new HashMap<>();
        int txType;

        int count = 0;
        long start = 0;
        long ledgerUse = 0;
        long verifyUse = 0;
        long outHashSetUse = 0;
        long getTxUse = 0;
        long sleepTIme = 0;
        long whileTime = 0;
        long startWhile = System.currentTimeMillis();
        long sizeTime = 0;
        long failed1Use = 0;
        long addTime = 0;
        // 为本次打包区块增加一个合约的临时余额区，用于记录本次合约地址余额的变化
        contractService.createContractTempBalance();
        while (true) {
            isCorrectContractTransfer = true;

            if ((self.getPackEndTime() - TimeService.currentTimeMillis()) <= 500L) {
                break;
            }
            start = System.nanoTime();
            Transaction tx = txMemoryPool.get();
            getTxUse += (System.nanoTime() - start);
            if (tx == null) {
                try {
                    sleepTIme += 100;
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Log.error("packaging error ", e);
                }
                continue;
            }

            txType = tx.getType();

            start = System.nanoTime();
            long txSize = tx.size();
            sizeTime += (System.nanoTime() - start);
            if ((totalSize + txSize) > ProtocolConstant.MAX_BLOCK_SIZE) {
                txMemoryPool.addInFirst(tx, false);
                break;
            }
            count++;
            start = System.nanoTime();
            Transaction repeatTx = ledgerService.getTx(tx.getHash());
            ledgerUse += (System.nanoTime() - start);
            if (repeatTx != null) {
                continue;
            }

            ValidateResult result = ValidateResult.getSuccessResult();
            if (!tx.isSystemTx()) {
                start = System.nanoTime();
                result = ledgerService.verifyCoinData(tx, toMaps, fromSet);
                verifyUse += (System.nanoTime() - start);
            }
            start = System.nanoTime();
            if (result.isFailed()) {
                if (tx == null) {
                    continue;
                }
                if (result.getErrorCode().equals(TransactionErrorCode.ORPHAN_TX)) {
                    txMemoryPool.add(tx, true);
                }
                failed1Use += (System.nanoTime() - start);
                continue;
            }
            start = System.nanoTime();
            if (!outHashSet.add(tx.getHash())) {
                outHashSetUse += (System.nanoTime() - start);
                Log.warn("重复的交易");
                continue;
            }
            outHashSetUse += (System.nanoTime() - start);


            // 打包时发现智能合约交易就调用智能合约
            callContractResult = contractService.invokeContract(tx, height, stateRoot);
            Log.info("=========================================doPacking stateRoot: " + Hex.encode(stateRoot));
            contractResult = callContractResult.getData();
            if(contractResult != null) {
                stateRoot = contractResult.getStateRoot();
                transfers = contractResult.getTransfers();
                Log.info("=========================================doPacking result StateRoot: " + Hex.encode(stateRoot));
                // 合约调用失败，且调用者存在资金转入合约地址，创建一笔合约内部转账，退回这笔资金
                if(!contractResult.isSuccess() && contractResult.getValue() > 0 && tx instanceof CallContractTransaction) {
                    CallContractTransaction callContractTx = (CallContractTransaction) tx;
                    CallContractData data = callContractTx.getTxData();
                    long price = data.getPrice();
                    Na transferFee = Na.valueOf(LongUtils.mul(ContractConstant.CONTRACT_TRANSFER_GAS_COST, price));
                    Na sendBack = Na.valueOf(contractResult.getValue());

                    if(sendBack.compareTo(transferFee) <= 0) {
                        transferFee = sendBack;
                    }
                    ContractTransfer transfer = new ContractTransfer(data.getContractAddress(), data.getSender(), sendBack, transferFee, true);
                    transfers.add(transfer);
                }
                // 创建合约转账交易
                if(transfers != null && transfers.size() > 0) {
                    // 合约转账使用的交易时间为区块时间
                    successContractTransferTxs = new HashMap<>();
                    Result<ContractTransferTransaction> contractTransferResult;
                    ContractTransferTransaction contractTransferTx;
                    for(ContractTransfer transfer : transfers) {
                        contractTransferResult = contractService.createContractTransferTx(transfer, bd.getTime(), toMaps, contractUsedCoinMap);
                        if(contractTransferResult.isFailed()) {
                            contractService.rollbackContractTransferTxs(successContractTransferTxs, toMaps, fromSet, contractUsedCoinMap);
                            isCorrectContractTransfer = false;
                            break;
                        }

                        contractTransferTx = contractTransferResult.getData();
                        result = contractService.verifyContractTransferCoinData(contractTransferTx, toMaps, fromSet);
                        if(result.isFailed()) {
                            // 如果转账出现错误，跳过整笔合约交易
                            // 回滚内部转账交易
                            contractService.rollbackContractTransferTx(contractTransferTx, toMaps, fromSet, contractUsedCoinMap);
                            contractService.rollbackContractTransferTxs(successContractTransferTxs, toMaps, fromSet, contractUsedCoinMap);
                            isCorrectContractTransfer = false;
                            break;
                        } else {
                            // 保存内部转账交易hash和外部合约交易hash
                            transfer.setHash(contractTransferTx.getHash());
                            transfer.setOrginHash(tx.getHash());
                            successContractTransferTxs.put(contractTransferTx.getHash().getDigestHex(), contractTransferTx);
                            contractTransferTxTotalSize += contractTransferTx.size();
                        }

                    }

                    // 如果合约内部转账出现错误，跳过整笔合约交易
                    if(!isCorrectContractTransfer) {
                        // 回滚临时余额
                        contractService.rollbackContractTempBalance(tx, contractResult);
                        // 清除toMaps和fromSet的这条交易的记录
                        contractService.rollbackVerifyData(tx, toMaps, fromSet);
                        Log.warn(result.getMsg());
                        continue;
                    }

                    // 整笔合约交易的大小 = 合约交易的大小 + 合约内部产生的转账交易的大小
                    if ((totalSize + contractTransferTxTotalSize + txSize) > ProtocolConstant.MAX_BLOCK_SIZE) {
                        // 回滚整笔交易的临时余额
                        contractService.rollbackContractTempBalance(tx, contractResult);

                        // 回滚内部转账交易
                        contractService.rollbackContractTransferTxs(successContractTransferTxs, toMaps, fromSet, contractUsedCoinMap);
                        txMemoryPool.addInFirst(tx, false);
                        break;
                    }
                    totalSize += contractTransferTxTotalSize;

                    packingTxList.addAll(successContractTransferTxs.values());
                }
                if(contractResult.isSuccess()) {
                    contractEvents = contractResult.getEvents();
                    if(contractEvents != null && contractEvents.size() > 0) {
                        //TODO pierre 发送合约事件
                    }
                }
                // 更新世界状态根
                bd.setStateRoot(stateRoot);
                // 这笔交易的合约执行结果保存在DB中, 另外保存在交易对象中，用于计算退还剩余的Gas --> method: addConsensusTx
                ContractTransaction contractTx = (ContractTransaction) tx;
                contractTx.setContractResult(contractResult);
                contractService.saveContractExecuteResult(tx.getHash(), contractResult);
            }

            tx.setBlockHeight(bd.getHeight());
            start = System.nanoTime();
            packingTxList.add(tx);
            addTime += (System.nanoTime() - start);

            totalSize += txSize;
        }
        // 打包结束后移除临时余额区
        contractService.removeContractTempBalance();

        whileTime = System.currentTimeMillis() - startWhile;
        ValidateResult validateResult = null;
        int failedCount = 0;
        long failedUse = 0;

        start = System.nanoTime();
        while (null == validateResult || validateResult.isFailed()) {
            failedCount++;
            validateResult = transactionService.conflictDetect(packingTxList);
            if (validateResult.isFailed()) {
                if (validateResult.getData() instanceof Transaction) {
                    packingTxList.remove(validateResult.getData());
                } else if (validateResult.getData() instanceof List) {
                    List<Transaction> list = (List<Transaction>) validateResult.getData();
                    if (list.size() == 2) {
                        packingTxList.remove(list.get(1));
                    } else {
                        packingTxList.removeAll(list);
                    }
                } else if (validateResult.getData() == null) {
                    Log.error("Cann't find the wrong transaction!");
                }
            }
        }
        // 组装CoinBase交易，另外合约调用退还剩余的Gas
        failedUse = System.nanoTime() - start;

        start = System.nanoTime();
        addConsensusTx(bestBlock, packingTxList, self, round);
        long consensusTxUse = System.nanoTime() - start;
        bd.setTxList(packingTxList);


        start = System.nanoTime();
        Block newBlock = ConsensusTool.createBlock(bd, round.getLocalPacker());
        long createBlockUser = System.nanoTime() - start;
        Log.info("make block height:" + newBlock.getHeader().getHeight() + ",txCount: " + newBlock.getTxs().size() + " , block size: " + newBlock.size() + " , time:" + DateUtil.convertDate(new Date(newBlock.getHeader().getTime())) + ",packEndTime:" +
                DateUtil.convertDate(new Date(self.getPackEndTime())));

        Log.info("=========================================newBlock height: " + newBlock.getHeader().getHeight()
            + ", newBlock hash: " + newBlock.getHeader().getHash().getDigestHex()
            + ", newBlock stateRoot: " + Hex.encode(newBlock.getHeader().getStateRoot()));

        Log.info("\ncheck count:" + count + "\ngetTxUse:" + getTxUse / 1000000 + " ,\nledgerExistUse:" + ledgerUse / 1000000 + ", \nverifyUse:" + verifyUse / 1000000 + " ,\noutHashSetUse:" + outHashSetUse / 1000000 + " ,\nfailedTimes:" + failedCount + ", \nfailedUse:" + failedUse / 1000000
                + " ,\nconsensusTx:" + consensusTxUse / 1000000 + ", \nblockUse:" + createBlockUser / 1000000 + ", \nsleepTIme:" + sleepTIme + ",\nwhileTime:" + whileTime
                + ", \naddTime:" + addTime / 1000000 + " ,\nsizeTime:" + sizeTime / 1000000 + " ,\nfailed1Use:" + failed1Use / 1000000);
        return newBlock;
    }



    /**
     * CoinBase transaction & Punish transaction
     *
     * @param bestBlock local highest block
     * @param txList    all tx of block
     * @param self      agent meeting data
     */
    private void addConsensusTx(Block bestBlock, List<Transaction> txList, MeetingMember self, MeetingRound round) throws NulsException, IOException {
        CoinBaseTransaction coinBaseTransaction = ConsensusTool.createCoinBaseTx(self, txList, round, bestBlock.getHeader().getHeight() + 1 + PocConsensusConstant.COINBASE_UNLOCK_HEIGHT);
        txList.add(0, coinBaseTransaction);
        punishTx(bestBlock, txList, self, round);
    }

    private void punishTx(Block bestBlock, List<Transaction> txList, MeetingMember self, MeetingRound round) throws NulsException, IOException {
        YellowPunishTransaction yellowPunishTransaction = ConsensusTool.createYellowPunishTx(bestBlock, self, round);
        if (null == yellowPunishTransaction) {
            return;
        }
        txList.add(yellowPunishTransaction);
        //当连续100个黄牌时，给出一个红牌
        //When 100 yellow CARDS in a row, give a red card.
        List<byte[]> addressList = yellowPunishTransaction.getTxData().getAddressList();
        Set<Integer> punishedSet = new HashSet<>();
        for (byte[] address : addressList) {
            MeetingMember member = round.getMemberByAgentAddress(address);
            if (null == member) {
                member = round.getPreRound().getMemberByAgentAddress(address);
            }
            if (member.getCreditVal() <= PocConsensusConstant.RED_PUNISH_CREDIT_VAL) {
                if (!punishedSet.add(member.getPackingIndexOfRound())) {
                    continue;
                }
                if (member.getAgent().getDelHeight() > 0L) {
                    continue;
                }
                RedPunishTransaction redPunishTransaction = new RedPunishTransaction();
                RedPunishData redPunishData = new RedPunishData();
                redPunishData.setAddress(address);
                redPunishData.setReasonCode(PunishReasonEnum.TOO_MUCH_YELLOW_PUNISH.getCode());
                redPunishTransaction.setTxData(redPunishData);
                CoinData coinData = ConsensusTool.getStopAgentCoinData(redPunishData.getAddress(), PocConsensusConstant.RED_PUNISH_LOCK_TIME);
                redPunishTransaction.setCoinData(coinData);
                redPunishTransaction.setHash(NulsDigestData.calcDigestData(redPunishTransaction.serializeForHash()));
                txList.add(redPunishTransaction);
            }
        }
    }
}
