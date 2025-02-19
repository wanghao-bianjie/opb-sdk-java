package irita.sdk;

import irita.sdk.client.IritaClient;
import irita.sdk.config.ClientConfig;
import irita.sdk.config.OpbConfig;
import irita.sdk.constant.enums.BroadcastMode;
import irita.sdk.key.KeyInfo;
import irita.sdk.key.KeyManager;
import irita.sdk.key.KeyManagerFactory;
import irita.sdk.model.BaseTx;
import irita.sdk.model.Fee;
import irita.sdk.model.ResultTx;
import irita.sdk.module.nft.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NftDemo {
    private KeyManager km;
    private NftClient nftClient;
    private final BaseTx baseTx = new BaseTx(200000, new Fee("200000", "uirita"), BroadcastMode.Commit);

    @BeforeEach
    public void init() {
        //更换为自己链上地址的助记词
        String mnemonic = "opera vivid pride shallow brick crew found resist decade neck expect apple chalk belt sick author know try tank detail tree impact hand best";
        km = KeyManagerFactory.createDefault();
        km.recover(mnemonic);

        //连接测试网（连接主网请参考README.md）
        String nodeUri = "http://47.100.192.234:26657";
        String grpcAddr = "47.100.192.234:9090";
        String chainId = "testing";
        ClientConfig clientConfig = new ClientConfig(nodeUri, grpcAddr, chainId);
        //测试网为null，主网请参考README.md
        OpbConfig opbConfig = null;

        IritaClient client = new IritaClient(clientConfig, opbConfig, km);
        nftClient = client.getNftClient();
        //判断由助记词恢复的是否为预期的链上地址
        assertEquals("iaa1ytemz2xqq2s73ut3ys8mcd6zca2564a5lfhtm3", km.getCurrentKeyInfo().getAddress());
    }

    @Test
    @Disabled
    public void testNft() throws IOException {
        String denomID = "testdenom" + new Random().nextInt(1000);
        String denomName = "test_name";
        String schema = "no shcema";
        String symbol = "symbol";
        //是否限制发行
        //true  只有 Denom 的所有者可以发行此类别的 NFT 给自己（发行到别的地址也是不可以的）
        //false 任何人都可以发行 NFT
        boolean mintRestricted = false;
        //是否限制更新 NFT
        //true  任何人都不可以更新 NFT
        //false 只有此 NFT 的所有者才能更新
        boolean updateRestricted = false;

        //issue denom 创建 denom
        IssueDenomRequest req = new IssueDenomRequest()
                .setId(denomID)
                .setName(denomName)
                .setSchema(schema)
                .setSymbol(symbol)
                .setMintRestricted(mintRestricted)
                .setUpdateRestricted(updateRestricted);
        ResultTx resultTx = nftClient.issueDenom(req, baseTx);
        assertNotNull(resultTx.getResult().getHash());

        //query denom 通过denomID查询denom信息
        QueryDenomResp denom = nftClient.queryDenom(denomID);
        assertEquals(denomID, denom.getId());
        assertEquals(denomName, denom.getName());
        assertEquals(schema, denom.getSchema());
        assertEquals(symbol, denom.getSymbol());
        assertEquals(mintRestricted, denom.isMintRestricted());
        assertEquals(updateRestricted, denom.isUpdateRestricted());

        KeyInfo keyInfo = km.getCurrentKeyInfo();
        assertEquals(keyInfo.getAddress(), denom.getCreator());

        //transfer denom 更改denom的owner
        String denomOwnerNew = "iaa14s9hekvzhtf3y3962zn3vzv45k0ay7mguyqhrl";
        TransferDenomRequest transferDenomReq = new TransferDenomRequest()
                .setId(denomID)
                .setRecipient(denomOwnerNew);
        resultTx = nftClient.transferDenom(transferDenomReq, baseTx);
        assertNotNull(resultTx);

        //query denom 通过denomID查询denom信息
        denom = nftClient.queryDenom(denomID);
        assertEquals(denomOwnerNew, denom.getCreator());

        //mint nft 在上面创建的denom下发行2个nft
        String nftID1 = "test1" + new Random().nextInt(1000);
        String nftID2 = "test2" + new Random().nextInt(1000);
        String nftName = "test_name";
        String uri = "https://www.baidu.com";
        String data = "any data";
        MintNFTRequest mintReq = new MintNFTRequest()
                .setDenom(denomID)
                .setId(nftID1)
                .setName(nftName)
                .setUri(uri)
                .setData(data)
                .setRecipient(keyInfo.getAddress());//nft的接收地址
        //mint nft1
        resultTx = nftClient.mintNft(mintReq, baseTx);
        assertNotNull(resultTx.getResult().getHash());
        //mint nft2
        mintReq.setId(nftID2);
        resultTx = nftClient.mintNft(mintReq, baseTx);
        assertNotNull(resultTx.getResult().getHash());

        //query nft1 通过denomID和nftID查询nft信息
        QueryNFTResp nft = nftClient.queryNFT(denomID, nftID1);
        assertEquals(nftID1, nft.getId());
        assertEquals(nftName, nft.getName());
        assertEquals(uri, nft.getUri());
        assertEquals(data, nft.getData());
        assertEquals(keyInfo.getAddress(), nft.getOwner());

        //transfer nft1 交易nft1到其他链上地址
        String reci = "iaa14s9hekvzhtf3y3962zn3vzv45k0ay7mguyqhrl";
        TransferNFTRequest transferReq = new TransferNFTRequest()
                .setDenom(denomID)
                .setId(nftID1)
                .setRecipient(reci);
        resultTx = nftClient.transferNFt(transferReq, baseTx);
        assertNotNull(resultTx.getResult().getHash());

        //query nft1 查询nft1的信息 判断nft的owner地址是否已改变
        nft = nftClient.queryNFT(denomID, nftID1);
        assertEquals(reci, nft.getOwner());

        //edit nft2 通过denomID和nftID修改nft的name/uri/data
        String newNftName = "test_name_new";
        String newUri = "https://www.baidu.com/new";
        String newData = "any data new";
        EditNFTRequest editReq = new EditNFTRequest()
                .setDenom(denomID)
                .setId(nftID2)
                .setName(newNftName)
                .setUri(newUri)
                .setData(newData);
        resultTx = nftClient.editNft(editReq, baseTx);
        assertNotNull(resultTx.getResult().getHash());

        //query collection 通过denomID查询集合 即该denom信息和其nft列表信息
        QueryCollectionResp queryCollectionResp = nftClient.queryCollection(denomID, null);
        assertNotNull(queryCollectionResp);
        assertEquals(2, queryCollectionResp.getNfts().size());

        //burn nft2 通过denomID和nftID销毁nft2
        BurnNFTRequest burnNFTReq = new BurnNFTRequest()
                .setDenom(denomID)
                .setId(nftID2);
        resultTx = nftClient.burnNft(burnNFTReq, baseTx);
        assertNotNull(resultTx.getResult().getHash());

        //query collection 查询集合，nft列表的数量减少1个
        queryCollectionResp = nftClient.queryCollection(denomID, null);
        assertNotNull(queryCollectionResp);
        assertEquals(1, queryCollectionResp.getNfts().size());

        //query denoms 查询所有denom列表
        List<QueryDenomResp> queryDenomResps = nftClient.queryDenoms(null);
        assertNotNull(queryDenomResps);

        //query owner 通过地址查询该地址拥有的nft列表 denomID可以指定也可以传空
        QueryOwnerResp queryOwnerResp = nftClient.queryOwner(denomID, reci);
        assertNotNull(queryOwnerResp);
        queryOwnerResp = nftClient.queryOwner("", reci);
        assertNotNull(queryOwnerResp);

        //query supply 查询该地址在该denom下拥有的nft数量
        long supply = nftClient.querySupply(denomID, reci);
        assertEquals(1, supply);
    }
}
