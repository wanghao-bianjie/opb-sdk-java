package irita.sdk;

import irita.sdk.client.IritaClient;
import irita.sdk.config.ClientConfig;
import irita.sdk.config.OpbConfig;
import irita.sdk.constant.enums.BroadcastMode;
import irita.sdk.key.KeyManager;
import irita.sdk.key.KeyManagerFactory;
import irita.sdk.model.BaseTx;
import irita.sdk.model.Coin;
import irita.sdk.model.Fee;
import irita.sdk.model.ResultTx;
import irita.sdk.module.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import proto.service.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServiceDemo {
    private KeyManager km;
    private ServiceClient serviceClient;
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
        serviceClient = client.getServiceClient();
        //判断由助记词恢复的是否为预期的链上地址
        assertEquals("iaa1ytemz2xqq2s73ut3ys8mcd6zca2564a5lfhtm3", km.getCurrentKeyInfo().getAddress());
    }

    @Test
    @Disabled
    public void testService() throws IOException {
        //定义服务
        String serviceName = "testservice" + new Random().nextInt(1000);
        String description = "使用空格拼接 param1 param2 两个字符串";
        String authorDescription = "authorDescription...";
        String schemas = "{\n" +
                "    \"input\": {\n" +
                "        \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
                "        \"title\": \"test service input body\",\n" +
                "        \"description\": \"test service input body specification\",\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": {\n" +
                "            \"param1\": {\n" +
                "                \"description\": \"param1...\",\n" +
                "                \"type\": \"string\"\n" +
                "            },\n" +
                "            \"param2\": {\n" +
                "                \"description\": \"param2...\",\n" +
                "                \"type\": \"string\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"required\": [\n" +
                "            \"param1\",\n" +
                "            \"param2\"\n" +
                "        ]\n" +
                "    },\n" +
                "    \"output\": {\n" +
                "        \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
                "        \"title\": \"test service output body\",\n" +
                "        \"description\": \"test service output body specification\",\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": {\n" +
                "            \"data\": {\n" +
                "                \"description\": \"result data...\",\n" +
                "                \"type\": \"string\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"required\": [\n" +
                "            \"data\"\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        DefineServiceRequest defineServiceRequest = new DefineServiceRequest()
                .setServiceName(serviceName)
                .setDescription(description)
                .setAuthorDescription(authorDescription)
                .setSchemas(schemas)
                .setTags(tags);
        ResultTx resultTx = serviceClient.defineService(defineServiceRequest, baseTx);
        assertNotNull(resultTx.getResult().getHash());

        Service.ServiceDefinition definition = serviceClient.queryServiceDefinition(serviceName);
        assertNotNull(definition);

        //绑定服务
        Coin deposit = new Coin("upoint", "5000000000");
        String pricing = "{\"price\":\"1upoint\"}";
        long qos = 50;
        String options = "{}";
        String provider = km.getCurrentKeyInfo().getAddress();
        BindServiceRequest bindServiceRequest = new BindServiceRequest()
                .setServiceName(serviceName)
                .setDeposit(deposit)
                .setPricing(pricing)
                .setOptions(options)
                .setQoS(qos)
                .setProvider(provider);
        resultTx = serviceClient.bindService(bindServiceRequest, baseTx);
        assertNotNull(resultTx.getResult().getHash());

        Service.ServiceBinding binding = serviceClient.queryServiceBinding(serviceName, km.getCurrentKeyInfo().getAddress());
        assertNotNull(binding);

        //服务调用
        List<String> providers = new ArrayList<>();
        providers.add(km.getCurrentKeyInfo().getAddress());
        //2个参数：param1,param2
        String input = "{\"header\":{},\"body\":{\"param1\":\"hello\",\"param2\":\"world\"}}";
        Coin serviceFeeCap = new Coin("upoint", "1");
        long timeout = 50;
        boolean repeated = false;
        long repeatedFrequency = 0;
        long repeatedTotal = 0;
        CallServiceRequest callServiceRequest = new CallServiceRequest()
                .setServiceName(serviceName)
                .setProviders(providers)
                .setInput(input)
                .setServiceFeeCap(serviceFeeCap)
                .setTimeout(timeout)
                .setRepeated(repeated)
                .setRepeatedFrequency(repeatedFrequency)
                .setRepeatedTotal(repeatedTotal);
        CallServiceResp callServiceResp = serviceClient.callService(callServiceRequest, baseTx);
        assertNotNull(callServiceResp);
        System.out.println(callServiceResp.getReqCtxId());
        System.out.println(callServiceResp.getResultTx().getResult().getHash());


        String requestId = "";
        List<Service.Request> requestList = serviceClient.queryServiceRequests(serviceName, km.getCurrentKeyInfo().getAddress());
        for (Service.Request request : requestList) {
            requestId = request.getId();
            String requestInput = request.getInput();
            assertEquals(input, requestInput);
            //此处省略解析 requestInput 的 json 字符串得到
            String param1 = "hello";
            String param2 = "world";
            String data = param1 + " " + param2;
            String result = "{\"code\":200,\"message\":\"success\"}";
            String output = "{\"header\":{},\"body\":{\"data\":\"" + data + "\"}}";
            //服务响应
            ResponseServiceRequest responseServiceRequest = new ResponseServiceRequest()
                    .setRequestId(requestId)
                    .setResult(result)
                    .setOutput(output);
            resultTx = serviceClient.responseService(responseServiceRequest, baseTx);
            assertNotNull(resultTx);

            Service.Response response = serviceClient.queryServiceResponse(requestId);
            assertNotNull(response);
            assertEquals(output,response.getOutput());
            System.out.println(response.getOutput());
        }
    }
}
