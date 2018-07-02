package io.nuls.contract;

import io.nuls.db.model.Entry;
import io.nuls.db.service.BatchOperation;
import io.nuls.db.service.DBService;
import io.nuls.kernel.model.Result;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class KVStorageServiceImpl implements DBService {

    private String dir = "/tmp/";

    @Override
    public Result createArea(String areaName) {
        return null;
    }

    @Override
    public Result createArea(String areaName, Long cacheSize) {
        return null;
    }

    @Override
    public Result createArea(String areaName, Comparator<byte[]> comparator) {
        return null;
    }

    @Override
    public Result createArea(String areaName, Long cacheSize, Comparator<byte[]> comparator) {
        return null;
    }

    @Override
    public String[] listArea() {
        return new String[0];
    }

    @Override
    public Result put(String area, byte[] key, byte[] value) {
        try {
            FileUtils.writeByteArrayToFile(getFile(key), value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public <T> Result putModel(String area, byte[] key, T value) {
        return null;
    }

    @Override
    public Result delete(String area, byte[] key) {
        return null;
    }


    @Override
    public byte[] get(String area, byte[] key) {
        try {
            return FileUtils.readFileToByteArray(getFile(key));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T getModel(String area, byte[] key, Class<T> clazz) {
        return null;
    }

    @Override
    public Object getModel(String area, byte[] key) {
        return null;
    }

    @Override
    public Set<byte[]> keySet(String area) {
        return null;
    }

    @Override
    public List<byte[]> keyList(String area) {
        return null;
    }

    @Override
    public List<byte[]> valueList(String area) {
        return null;
    }

    @Override
    public Set<Entry<byte[], byte[]>> entrySet(String area) {
        return null;
    }

    @Override
    public List<Entry<byte[], byte[]>> entryList(String area) {
        return null;
    }

    @Override
    public <T> List<Entry<byte[], T>> entryList(String area, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> List<T> values(String area, Class<T> clazz) {
        return null;
    }

    @Override
    public BatchOperation createWriteBatch(String area) {
        return null;
    }

    public File getFile(byte[] key) {
        String file = dir + DigestUtils.md5Hex(key);
        return new File(file);
    }

    @Override
    public Result destroyArea(String areaName) {
        return null;
    }

    @Override
    public Result clearArea(String area) {
        //TODO pierre auto-generated method stub
        return null;
    }

}
