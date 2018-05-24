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

    private static final String ADDRESS = "crowdsale_address";
    private static final String SENDER = "ccc_address";

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
        programCreate.setContractAddress(ADDRESS.getBytes());
        programCreate.setSender(SENDER.getBytes());
        programCreate.setPrice(0);
        programCreate.setNaLimit(1000000);
        programCreate.setNumber(1);
        programCreate.setContractCode(contractCode);
        programCreate.args("10", Hex.toHexString("wallet_address".getBytes()), Hex.toHexString("token_address".getBytes()));
        System.out.println(programCreate);

        byte[] prevStateRoot = Hex.decode("e3ddc1a2bb001d75b038ade8aaef3868e75be68eaf4010958f0b4587f844a906");

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
        programCall.setContractAddress(ADDRESS.getBytes());
        programCall.setSender(SENDER.getBytes());
        programCall.setValue(new BigInteger("1000"));
        programCall.setPrice(0);
        programCall.setNaLimit(1000000);
        programCall.setNumber(1);
        programCall.setMethodName("buyTokens");
        programCall.setMethodDesc("");
        programCall.args(Hex.toHexString(SENDER.getBytes()));
        System.out.println(programCall);

        byte[] prevStateRoot = Hex.decode("16f848285e84fe44cdf09e039c0e750bcfcf92c4395fa27bf50180d61465751b");

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        ProgramResult programResult = track.call(programCall);
        track.commit();

        System.out.println(programResult);
        System.out.println("stateRoot: " + Hex.toHexString(track.getRoot()));
        System.out.println();
    }

}
