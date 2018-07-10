package io.nuls.contract;

import io.nuls.contract.util.VMContext;
import io.nuls.contract.vm.program.*;
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
import java.util.ArrayList;
import java.util.List;

public class ContractTest {

    private VMContext vmContext;
    private DBService dbService;
    private ProgramExecutor programExecutor;

    private static final String ADDRESS = "token_address";
    private static final String SENDER = "crowdsale_address";
    private static final String BUYER = "buyer_address";

    @Before
    public void setUp() {
        dbService = new LevelDBServiceImpl();
        programExecutor = new ProgramExecutorImpl(vmContext, dbService);
    }

    @Test
    public void testCreate() throws IOException {
        InputStream in = new FileInputStream(ContractTest.class.getResource("/token_contract").getFile());
        //InputStream in = new FileInputStream(ContractTest.class.getResource("/").getFile() + "../contract.jar");
        byte[] contractCode = IOUtils.toByteArray(in);

        ProgramCreate programCreate = new ProgramCreate();
        programCreate.setContractAddress(ADDRESS.getBytes());
        programCreate.setSender(SENDER.getBytes());
        programCreate.setPrice(0);
        programCreate.setGasLimit(1000000);
        programCreate.setNumber(1);
        programCreate.setContractCode(contractCode);
        //programCreate.args();
        System.out.println(programCreate);

        byte[] prevStateRoot = Hex.decode("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.create(programCreate);
        track.commit();

        System.out.println(programResult);
        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

    @Test
    public void testCall() throws IOException {
        ProgramCall programCall = new ProgramCall();
        programCall.setContractAddress(ADDRESS.getBytes());
        programCall.setSender(SENDER.getBytes());
        programCall.setPrice(0);
        programCall.setGasLimit(1000000);
        programCall.setNumber(1);
        programCall.setMethodName("mint");
        programCall.setMethodDesc("");
        programCall.args(Hex.toHexString(BUYER.getBytes()), "1000");
        System.out.println(programCall);

        byte[] prevStateRoot = Hex.decode("959c21fc0539d7a8df5da6ed705288f2c0c5b8bbda2bee8ae25a82cd541e061d");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.call(programCall);
        track.commit();

        System.out.println(programResult);
        System.out.println("pierre - stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();

        programCall.setMethodName("balanceOf");
        programCall.setMethodDesc("");
        programCall.args(Hex.toHexString(BUYER.getBytes()));
        System.out.println(programCall);

        track = programExecutor.begin(track.getRoot());
        programResult = track.call(programCall);
        track.commit();

        System.out.println(programResult);
        System.out.println("pierre - stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

    @Test
    public void testAddBalance() throws IOException {
        ProgramCall programCall = new ProgramCall();
        programCall.setContractAddress(ADDRESS.getBytes());
        programCall.setSender(SENDER.getBytes());
        programCall.setPrice(0);
        programCall.setGasLimit(1000000);
        programCall.setNumber(1);
        programCall.setMethodName("_payable");
        programCall.setMethodDesc("()V");
        programCall.setValue(new BigInteger("100"));
        System.out.println(programCall);

        byte[] prevStateRoot = Hex.decode("08cf9c62806d73378bf64f03ea401fbbd08a318ec580d2bfc4c45641b0921a9e");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.call(programCall);
        track.commit();

        System.out.println(programResult);
        System.out.println("pierre - stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

    @Test
    public void testStop() throws IOException {
        byte[] prevStateRoot = Hex.decode("08cf9c62806d73378bf64f03ea401fbbd08a318ec580d2bfc4c45641b0921a9e");
        byte[] address = ADDRESS.getBytes();
        byte[] sender = SENDER.getBytes();

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.stop(address, sender);
        track.commit();

        System.out.println(programResult);
        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

    @Test
    public void testStatus() throws IOException {
        byte[] prevStateRoot = Hex.decode("08cf9c62806d73378bf64f03ea401fbbd08a318ec580d2bfc4c45641b0921a9e");
        byte[] address = ADDRESS.getBytes();
        byte[] sender = SENDER.getBytes();

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramStatus programStatus = track.status(address);

        System.out.println(programStatus);
        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

    @Test
    public void testTransactions() {
        List<ProgramCall> transactions = new ArrayList<>();

        ProgramCall programCall = new ProgramCall();
        programCall.setContractAddress(ADDRESS.getBytes());
        programCall.setSender(SENDER.getBytes());
        programCall.setPrice(0);
        programCall.setGasLimit(1000000);
        programCall.setNumber(1);
        programCall.setMethodName("balanceOf");
        programCall.setMethodDesc("");
        programCall.args(Hex.toHexString(ADDRESS.getBytes()));
        System.out.println(programCall);
        transactions.add(programCall);

        ProgramCall programCall1 = new ProgramCall();
        programCall1.setContractAddress(ADDRESS.getBytes());
        programCall1.setSender(SENDER.getBytes());
        programCall1.setPrice(0);
        programCall1.setGasLimit(1000000);
        programCall1.setNumber(1);
        programCall1.setMethodName("balanceOf");
        programCall1.setMethodDesc("");
        programCall1.args(Hex.toHexString(ADDRESS.getBytes()));
        System.out.println(programCall1);
        transactions.add(programCall1);

        byte[] prevStateRoot = Hex.decode("08cf9c62806d73378bf64f03ea401fbbd08a318ec580d2bfc4c45641b0921a9e");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        for (ProgramCall transaction : transactions) {
            ProgramExecutor txTrack = track.startTracking();
            ProgramResult programResult = txTrack.call(transaction);
            txTrack.commit();

            System.out.println(programResult);
            System.out.println();
        }
        track.commit();

        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

    @Test
    public void testMethod() throws IOException {
        byte[] prevStateRoot = Hex.decode("08cf9c62806d73378bf64f03ea401fbbd08a318ec580d2bfc4c45641b0921a9e");
        byte[] address = ADDRESS.getBytes();
        byte[] sender = SENDER.getBytes();

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        List<ProgramMethod> methods = track.method(address);
        //track.commit();

        for (ProgramMethod method : methods) {
            System.out.println(method);
        }
    }

    @Test
    public void testTransfer() throws IOException {
        ProgramCall programCall = new ProgramCall();
        programCall.setContractAddress(ADDRESS.getBytes());
        programCall.setSender(SENDER.getBytes());
        programCall.setPrice(0);
        programCall.setGasLimit(1000000);
        programCall.setNumber(1);
        programCall.setMethodName("transfer");
        programCall.setMethodDesc("");
        programCall.args(Hex.toHexString(BUYER.getBytes()), "-1000");
        System.out.println(programCall);

        byte[] prevStateRoot = Hex.decode("959c21fc0539d7a8df5da6ed705288f2c0c5b8bbda2bee8ae25a82cd541e061d");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.call(programCall);
        track.commit();

        System.out.println(programResult);
        System.out.println("pierre - stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();

        programCall.setMethodName("balanceOf");
        programCall.setMethodDesc("");
        programCall.args(Hex.toHexString(BUYER.getBytes()));
        System.out.println(programCall);

        track = programExecutor.begin(track.getRoot());
        programResult = track.call(programCall);
        track.commit();

        System.out.println(programResult);
        System.out.println("pierre - stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

}
