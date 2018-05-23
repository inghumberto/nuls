package io.nuls.contract.util;


import io.nuls.contract.entity.BlockHeader;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.kernel.context.NulsContext;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.lite.core.SpringLiteContext;
import io.nuls.kernel.model.NulsDigestData;
import io.nuls.kernel.model.Result;
import io.nuls.protocol.service.BlockService;

import java.io.IOException;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date: 2018/5/2
 */
public enum VMContext {
    CONTEXT;

    private BlockService blockService = SpringLiteContext.getBean(BlockService.class);

    /**
     * @param hash
     * @return
     * @throws NulsException
     * @throws IOException
     */
    public BlockHeader getBlockHeader(String hash) throws NulsException, IOException {
        if(StringUtils.isBlank(hash)) {
            return null;
        }
        NulsDigestData nulsDigestData = NulsDigestData.fromDigestHex(hash);
        Result<io.nuls.kernel.model.BlockHeader> blockHeaderResult = blockService.getBlockHeader(nulsDigestData);
        if(blockHeaderResult == null || blockHeaderResult.getData() == null) {
            return null;
        }
        BlockHeader header = new BlockHeader(blockHeaderResult.getData());
        return header;

    }

    /**
     * @param height
     * @return
     * @throws NulsException
     * @throws IOException
     */
    public BlockHeader getBlockHeader(long height) throws NulsException, IOException {
        if(height < 0 || NulsContext.getInstance().getBestHeight() < height) {
            return null;
        }
        Result<io.nuls.kernel.model.BlockHeader> blockHeaderResult = blockService.getBlockHeader(height);
        if(blockHeaderResult == null || blockHeaderResult.getData() == null) {
            return null;
        }
        BlockHeader header = new BlockHeader(blockHeaderResult.getData());
        return header;
    }

    /**
     * get the newest block header
     * @return
     * @throws IOException
     */
    public BlockHeader getBlockHeader() throws IOException {
        return new BlockHeader(NulsContext.getInstance().getBestBlock().getHeader());
    }
}
