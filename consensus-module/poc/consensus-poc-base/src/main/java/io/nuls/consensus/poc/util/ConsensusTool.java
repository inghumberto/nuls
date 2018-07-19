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
package io.nuls.consensus.poc.util;

import io.nuls.account.constant.AccountErrorCode;
import io.nuls.account.model.Account;
import io.nuls.account.service.AccountService;
import io.nuls.consensus.poc.constant.PocConsensusConstant;
import io.nuls.consensus.poc.context.PocConsensusContext;
import io.nuls.consensus.poc.model.BlockData;
import io.nuls.consensus.poc.model.BlockRoundData;
import io.nuls.consensus.poc.model.MeetingMember;
import io.nuls.consensus.poc.model.MeetingRound;
import io.nuls.consensus.poc.protocol.entity.Agent;
import io.nuls.consensus.poc.protocol.entity.Deposit;
import io.nuls.consensus.poc.protocol.entity.YellowPunishData;
import io.nuls.consensus.poc.protocol.tx.CreateAgentTransaction;
import io.nuls.consensus.poc.protocol.tx.DepositTransaction;
import io.nuls.consensus.poc.protocol.tx.YellowPunishTransaction;
import io.nuls.consensus.poc.storage.service.AgentStorageService;
import io.nuls.consensus.poc.storage.service.DepositStorageService;
import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.entity.tx.ContractTransaction;
import io.nuls.contract.entity.txdata.ContractData;
import io.nuls.contract.service.ContractService;
import io.nuls.core.tools.array.ArraysTool;
import io.nuls.core.tools.calc.DoubleUtils;
import io.nuls.core.tools.calc.LongUtils;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.context.NulsContext;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.exception.NulsRuntimeException;
import io.nuls.kernel.func.TimeService;
import io.nuls.kernel.model.*;
import io.nuls.kernel.script.P2PKHScriptSig;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.kernel.utils.ByteArrayWrapper;
import io.nuls.kernel.utils.VarInt;
import io.nuls.ledger.service.LedgerService;
import io.nuls.protocol.model.SmallBlock;
import io.nuls.protocol.model.tx.CoinBaseTransaction;

import java.io.IOException;
import java.util.*;

/**
 * @author Niels
 */
public class ConsensusTool {

    private static AccountService accountService = NulsContext.getServiceBean(AccountService.class);
    private static AgentStorageService agentStorageService = NulsContext.getServiceBean(AgentStorageService.class);
    private static DepositStorageService depositStorageService = NulsContext.getServiceBean(DepositStorageService.class);
    private static LedgerService ledgerService = NulsContext.getServiceBean(LedgerService.class);
    /**
     * pierre add 合约服务接口
     */
    private static ContractService contractService = NulsContext.getServiceBean(ContractService.class);

    public static SmallBlock getSmallBlock(Block block) {
        SmallBlock smallBlock = new SmallBlock();
        smallBlock.setHeader(block.getHeader());
        List<NulsDigestData> txHashList = new ArrayList<>();
        for (Transaction tx : block.getTxs()) {
            txHashList.add(tx.getHash());
            if (tx.isSystemTx()) {
                smallBlock.addBaseTx(tx);
            }
        }
        smallBlock.setTxHashList(txHashList);
        return smallBlock;
    }

    public static Block createBlock(BlockData blockData, Account account) throws NulsException {
        if (null == account) {
            throw new NulsRuntimeException(AccountErrorCode.ACCOUNT_NOT_EXIST);
        }

        // Account cannot be encrypted, otherwise it will be wrong
        // 账户不能加密，否则抛错
        Result result = accountService.isEncrypted(account);
        if (result.isSuccess()) {
            throw new NulsRuntimeException(AccountErrorCode.ACCOUNT_IS_ALREADY_ENCRYPTED);
        }

        Block block = new Block();
        block.setTxs(blockData.getTxList());
        BlockHeader header = new BlockHeader();
        block.setHeader(header);
        try {
            block.getHeader().setExtend(blockData.getRoundData().serialize());
        } catch (IOException e) {
            Log.error(e);
            throw new NulsRuntimeException(e);
        }
        header.setHeight(blockData.getHeight());
        header.setTime(blockData.getTime());
        header.setPreHash(blockData.getPreHash());
        header.setTxCount(blockData.getTxList().size());
        List<NulsDigestData> txHashList = new ArrayList<>();
        for (int i = 0; i < blockData.getTxList().size(); i++) {
            Transaction tx = blockData.getTxList().get(i);
            tx.setBlockHeight(header.getHeight());
            txHashList.add(tx.getHash());
        }
        header.setMerkleHash(NulsDigestData.calcMerkleDigestData(txHashList));
        header.setHash(NulsDigestData.calcDigestData(block.getHeader()));

        P2PKHScriptSig scriptSig = new P2PKHScriptSig();

        NulsSignData signData = accountService.signDigest(header.getHash().getDigestBytes(), account.getEcKey());
        scriptSig.setSignData(signData);
        scriptSig.setPublicKey(account.getPubKey());
        header.setScriptSig(scriptSig);
        header.setStateRoot(blockData.getStateRoot());

        return block;
    }

    public static CoinBaseTransaction createCoinBaseTx(MeetingMember member, List<Transaction> txList, MeetingRound localRound, long unlockHeight) {
        CoinData coinData = new CoinData();
        // 合约剩余Gas退还/合约调用失败转入资金退还
        List<Coin> returnGasList = returnContractSenderNa(txList, unlockHeight);

        List<Coin> rewardList = calcReward(txList, member, localRound, unlockHeight);
        if(!returnGasList.isEmpty()) {
            Coin agentReward = rewardList.remove(0);
            rewardList.addAll(returnGasList);
            rewardList.sort(new Comparator<Coin>() {
                @Override
                public int compare(Coin o1, Coin o2) {
                    return Arrays.hashCode(o1.getOwner()) > Arrays.hashCode(o2.getOwner()) ? 1 : -1;
                }
            });
            rewardList.add(0, agentReward);
        }

        for (Coin coin : rewardList) {
            Log.info("=========================================coinBase: " + coin.toString());
            coinData.addTo(coin);
        }
        CoinBaseTransaction tx = new CoinBaseTransaction();
        tx.setTime(member.getPackEndTime());
        tx.setCoinData(coinData);
        try {
            tx.setHash(NulsDigestData.calcDigestData(tx.serializeForHash()));
        } catch (IOException e) {
            Log.error(e);
        }
        return tx;
    }

    private static List<Coin> returnContractSenderNa(List<Transaction> txList, long unlockHeight) {
        // 去重, 可能存在同一个sender发出的几笔合约交易，需要把退还的GasNa累加到一起
        Map<ByteArrayWrapper, Na> returnMap = new HashMap<>();
        List<Coin> returnList = new ArrayList<>();
        if(txList != null && txList.size() > 0) {
            int txType;
            for (Transaction tx : txList) {
                txType = tx.getType();
                if(txType == ContractConstant.TX_TYPE_CREATE_CONTRACT
                        || txType == ContractConstant.TX_TYPE_CALL_CONTRACT
                        || txType == ContractConstant.TX_TYPE_DELETE_CONTRACT) {
                    ContractTransaction contractTx = (ContractTransaction) tx;
                    ContractResult contractResult = contractTx.getContractResult();
                    if(contractResult == null) {
                        contractResult = contractService.getContractExecuteResult(tx.getHash());
                        if(contractResult == null) {
                            Log.error("get contract tx contractResult error: " + tx.getHash().getDigestHex());
                            continue;
                        }
                    }
                    contractTx.setContractResult(contractResult);

                    // 终止合约不消耗Gas，跳过
                    if(txType == ContractConstant.TX_TYPE_DELETE_CONTRACT) {
                        continue;
                    }
                    // 减差额作为退还Gas
                    ContractData contractData = (ContractData) tx.getTxData();
                    long realGasUsed = contractResult.getGasUsed();
                    long txGasUsed = contractData.getGasLimit();
                    long returnGas = 0;
                    if(txGasUsed > realGasUsed) {
                        returnGas = txGasUsed - realGasUsed;
                        ByteArrayWrapper sender = new ByteArrayWrapper(contractData.getSender());
                        Na senderNa = returnMap.get(sender);
                        if(senderNa == null) {
                            senderNa = Na.ZERO.add(Na.valueOf(LongUtils.mul(returnGas, contractData.getPrice())));
                        } else {
                            senderNa = senderNa.add(Na.valueOf(LongUtils.mul(returnGas, contractData.getPrice())));
                        }
                        returnMap.put(sender, senderNa);
                    }
                    // 合约调用失败，退还调用者转入合约地址的资金
                    /*if(!contractResult.isSuccess()) {
                        Na returnValue = Na.valueOf(contractResult.getValue());
                        Na senderNa = returnMap.get(sender);
                        if(senderNa == null) {
                            senderNa = Na.ZERO.add(returnValue);
                        } else {
                            senderNa = senderNa.add(returnValue);
                        }
                        returnMap.put(sender, senderNa);
                    }*/
                }
            }
            Set<Map.Entry<ByteArrayWrapper, Na>> entries = returnMap.entrySet();
            Coin returnCoin;
            for(Map.Entry<ByteArrayWrapper, Na> entry : entries) {
                returnCoin = new Coin(entry.getKey().getBytes(), entry.getValue(), unlockHeight);
                returnList.add(returnCoin);
            }
        }
        return returnList;
    }

    private static List<Coin> calcReward(List<Transaction> txList, MeetingMember self, MeetingRound localRound, long unlockHeight) {
        List<Coin> rewardList = new ArrayList<>();
        if (self.getOwnDeposit().getValue() == Na.ZERO.getValue()) {
            long totalFee = 0;
            for (Transaction tx : txList) {
                totalFee += tx.getFee().getValue();
            }
            if (totalFee == 0L) {
                return rewardList;
            }
            double caReward = totalFee;
            Coin agentReword = new Coin(self.getRewardAddress(), Na.valueOf((long) caReward), unlockHeight);
            rewardList.add(agentReword);
            return rewardList;
        }
        long totalFee = 0;
        for (Transaction tx : txList) {
            totalFee += tx.getFee().getValue();
        }
        double totalAll = DoubleUtils.mul(localRound.getMemberCount(), PocConsensusConstant.BLOCK_REWARD.getValue());
        double commissionRate = DoubleUtils.div(self.getCommissionRate(), 100, 2);
        double agentWeight = DoubleUtils.mul(DoubleUtils.sum(self.getOwnDeposit().getValue(), self.getTotalDeposit().getValue()), self.getCalcCreditVal());
        double blockReword = totalFee;
        if (localRound.getTotalWeight() > 0d && agentWeight > 0d) {
            blockReword = DoubleUtils.sum(blockReword, DoubleUtils.mul(totalAll, DoubleUtils.div(agentWeight, localRound.getTotalWeight())));
        }

        if (blockReword == 0d) {
            return rewardList;
        }

        long realTotalAllDeposit = self.getOwnDeposit().getValue() + self.getTotalDeposit().getValue();
        double caReward = DoubleUtils.mul(blockReword, DoubleUtils.div(self.getOwnDeposit().getValue(), realTotalAllDeposit));

        for (Deposit deposit : self.getDepositList()) {
            double weight = DoubleUtils.div(deposit.getDeposit().getValue(), realTotalAllDeposit);
            if (Arrays.equals(deposit.getAddress(), self.getAgentAddress())) {
                caReward = caReward + DoubleUtils.mul(blockReword, weight);
            } else {
                double reward = DoubleUtils.mul(blockReword, weight);
                double fee = DoubleUtils.mul(reward, commissionRate);
                caReward = caReward + fee;
                double hisReward = DoubleUtils.sub(reward, fee);
                if (hisReward == 0D) {
                    continue;
                }
                Na depositReward = Na.valueOf(DoubleUtils.longValue(hisReward));

                Coin rewardCoin = null;

                for (Coin coin : rewardList) {
                    if (Arrays.equals(coin.getOwner(), deposit.getAddress())) {
                        rewardCoin = coin;
                        break;
                    }
                }
                if (rewardCoin == null) {
                    rewardCoin = new Coin(deposit.getAddress(), depositReward, unlockHeight);
                    rewardList.add(rewardCoin);
                } else {
                    rewardCoin.setNa(rewardCoin.getNa().add(depositReward));
                }
            }
        }

        rewardList.sort(new Comparator<Coin>() {
            @Override
            public int compare(Coin o1, Coin o2) {
                return Arrays.hashCode(o1.getOwner()) > Arrays.hashCode(o2.getOwner()) ? 1 : -1;
            }
        });

        Coin agentReward = new Coin(self.getRewardAddress(), Na.valueOf(DoubleUtils.longValue(caReward)), unlockHeight);
        rewardList.add(0, agentReward);

        return rewardList;
    }


    public static YellowPunishTransaction createYellowPunishTx(Block preBlock, MeetingMember self, MeetingRound round) throws NulsException, IOException {
        BlockRoundData preBlockRoundData = new BlockRoundData(preBlock.getHeader().getExtend());
        if (self.getRoundIndex() - preBlockRoundData.getRoundIndex() > 1) {
            return null;
        }

        int yellowCount = 0;
        if (self.getRoundIndex() == preBlockRoundData.getRoundIndex() && self.getPackingIndexOfRound() != preBlockRoundData.getPackingIndexOfRound() + 1) {
            yellowCount = self.getPackingIndexOfRound() - preBlockRoundData.getPackingIndexOfRound() - 1;
        }

        if (self.getRoundIndex() != preBlockRoundData.getRoundIndex() && (self.getPackingIndexOfRound() != 1 || preBlockRoundData.getPackingIndexOfRound() != preBlockRoundData.getConsensusMemberCount())) {
            yellowCount = self.getPackingIndexOfRound() + preBlockRoundData.getConsensusMemberCount() - preBlockRoundData.getPackingIndexOfRound() - 1;
        }

        if (yellowCount == 0) {
            return null;
        }

        List<byte[]> addressList = new ArrayList<>();
        for (int i = 1; i <= yellowCount; i++) {
            int index = self.getPackingIndexOfRound() - i;
            if (index > 0) {
                MeetingMember member = round.getMember(index);
                if (member.getAgent() == null) {
                    continue;
                } else if (member.getAgent().getDelHeight() > 0) {
                    continue;
                }
                addressList.add(member.getAgentAddress());
            } else {
                MeetingRound preRound = round.getPreRound();
                MeetingMember member = preRound.getMember(index + preRound.getMemberCount());
                if (member.getAgent() == null || member.getAgent().getDelHeight() > 0) {
                    continue;
                }
                addressList.add(member.getAgentAddress());
            }
        }
        if (addressList.isEmpty()) {
            return null;
        }
        YellowPunishTransaction punishTx = new YellowPunishTransaction();
        YellowPunishData data = new YellowPunishData();
        data.setAddressList(addressList);
        punishTx.setTxData(data);
        punishTx.setTime(self.getPackEndTime());
        punishTx.setHash(NulsDigestData.calcDigestData(punishTx.serializeForHash()));
        return punishTx;
    }

    public static CoinData getStopAgentCoinData(Agent agent, long lockTime) throws IOException {
        if (null == agent) {
            return null;
        }
        NulsDigestData createTxHash = agent.getTxHash();
        CoinData coinData = new CoinData();
        List<Coin> toList = new ArrayList<>();
        toList.add(new Coin(agent.getAgentAddress(), agent.getDeposit(), TimeService.currentTimeMillis() + lockTime));
        coinData.setTo(toList);
        CreateAgentTransaction transaction = (CreateAgentTransaction) ledgerService.getTx(createTxHash);
        if (null == transaction) {
            throw new NulsRuntimeException(KernelErrorCode.DATA_NOT_FOUND);
        }
        List<Coin> fromList = new ArrayList<>();
        for (int index = 0; index < transaction.getCoinData().getTo().size(); index++) {
            Coin coin = transaction.getCoinData().getTo().get(index);
            if (coin.getNa().equals(agent.getDeposit()) && coin.getLockTime() == -1L) {
                coin.setOwner(ArraysTool.concatenate(transaction.getHash().serialize(), new VarInt(index).encode()));
                fromList.add(coin);
                break;
            }
        }
        if (fromList.isEmpty()) {
            throw new NulsRuntimeException(KernelErrorCode.DATA_ERROR);
        }
        coinData.setFrom(fromList);

        List<Deposit> deposits = PocConsensusContext.getChainManager().getMasterChain().getChain().getDepositList();
        List<String> addressList = new ArrayList<>();
        Map<String, Coin> toMap = new HashMap<>();
        for (Deposit deposit : deposits) {
            if (deposit.getDelHeight() > 0) {
                continue;
            }
            if (!deposit.getAgentHash().equals(agent.getTxHash())) {
                continue;
            }
            DepositTransaction dtx = (DepositTransaction) ledgerService.getTx(deposit.getTxHash());
            Coin fromCoin = null;
            for (Coin coin : dtx.getCoinData().getTo()) {
                if (!coin.getNa().equals(deposit.getDeposit()) || coin.getLockTime() != -1L) {
                    continue;
                }
                fromCoin = new Coin(ArraysTool.concatenate(dtx.getHash().serialize(), new VarInt(0).encode()), coin.getNa(), coin.getLockTime());
                fromCoin.setLockTime(-1L);
                fromList.add(fromCoin);
                break;
            }
            String address = AddressTool.getStringAddressByBytes(deposit.getAddress());
            Coin coin = toMap.get(address);
            if (null == coin) {
                coin = new Coin(deposit.getAddress(), deposit.getDeposit(), 0);
                addressList.add(address);
                toMap.put(address, coin);
            } else {
                coin.setNa(coin.getNa().add(fromCoin.getNa()));
            }
        }
        for (String address : addressList) {
            coinData.getTo().add(toMap.get(address));
        }

        return coinData;
    }

    public static CoinData getStopAgentCoinData(byte[] address, long lockTime) throws IOException {
        List<Agent> agentList = PocConsensusContext.getChainManager().getMasterChain().getChain().getAgentList();
        for (Agent agent : agentList) {
            if (agent.getDelHeight() > 0) {
                continue;
            }
            if (Arrays.equals(address, agent.getAgentAddress())) {
                return getStopAgentCoinData(agent, lockTime);
            }
        }
        return null;
    }

}

