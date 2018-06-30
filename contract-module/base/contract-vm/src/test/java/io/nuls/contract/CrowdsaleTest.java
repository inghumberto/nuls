package io.nuls.contract;

import io.nuls.contract.util.VMContext;
import io.nuls.contract.vm.program.ProgramCall;
import io.nuls.contract.vm.program.ProgramCreate;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.contract.vm.program.ProgramResult;
import io.nuls.contract.vm.program.impl.ProgramExecutorImpl;
import io.nuls.db.service.DBService;
import io.nuls.db.service.impl.LevelDBServiceImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

public class CrowdsaleTest {

    private VMContext vmContext;
    private DBService dbService;
    private ProgramExecutor programExecutor;

    private static final String CROWDSALE_ADDRESS = "crowdsale_address";
    private static final String TOKEN_ADDRESS = "token_address";
    private static final String WALLET_ADDRESS = "wallet_address";
    private static final String SENDER = "ccc_address";
    private static final String BUYER = "buyer_address";

    @Before
    public void setUp() {
        dbService = new LevelDBServiceImpl();
        programExecutor = new ProgramExecutorImpl(vmContext, dbService);
    }

    @Test
    public void testCreate() throws IOException {
        InputStream in = new FileInputStream(ContractTest.class.getResource("/crowdsale_contract").getFile());
        byte[] contractCode = IOUtils.toByteArray(in);

        ProgramCreate programCreate = new ProgramCreate();
        programCreate.setContractAddress(CROWDSALE_ADDRESS.getBytes());
        programCreate.setSender(SENDER.getBytes());
        programCreate.setPrice(0);
        programCreate.setGasLimit(1000000);
        programCreate.setNumber(1);
        programCreate.setContractCode(contractCode);
        programCreate.args("0", "20000", "10", Hex.toHexString(WALLET_ADDRESS.getBytes()), "20000000", Hex.toHexString(TOKEN_ADDRESS.getBytes()), "10000000");
        System.out.println(programCreate);

        byte[] prevStateRoot = Hex.decode("08cf9c62806d73378bf64f03ea401fbbd08a318ec580d2bfc4c45641b0921a9e");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.create(programCreate);
        track.commit();

        System.out.println(programResult);
        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

    @Test
    public void testBuyTokens() throws IOException {
        ProgramCall programCall = new ProgramCall();
        programCall.setContractAddress(CROWDSALE_ADDRESS.getBytes());
        programCall.setSender(SENDER.getBytes());
        programCall.setValue(new BigInteger("1000"));
        programCall.setPrice(0);
        programCall.setGasLimit(1000000);
        programCall.setNumber(1);
        programCall.setMethodName("buyTokens");
        programCall.setMethodDesc("");
        programCall.args(Hex.toHexString(BUYER.getBytes()));
        System.out.println(programCall);

        byte[] prevStateRoot = Hex.decode("f50a59ea0cde312187e49cdebb30f7c79d6c69c336a897efa534584e81fc48d4");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.call(programCall);
        track.commit();

        System.out.println(programResult);
        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();

        ProgramCall programCall1 = new ProgramCall();
        programCall1.setContractAddress(TOKEN_ADDRESS.getBytes());
        programCall1.setSender(SENDER.getBytes());
        programCall1.setPrice(0);
        programCall1.setGasLimit(1000000);
        programCall1.setNumber(1);
        programCall1.setMethodName("balanceOf");
        programCall1.setMethodDesc("");
        programCall1.args(Hex.toHexString(BUYER.getBytes()));

        track = programExecutor.begin(track.getRoot());
        programResult = track.call(programCall1);
        track.commit();

        System.out.println(programResult);
        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

}
