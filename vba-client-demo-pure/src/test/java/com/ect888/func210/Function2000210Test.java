package com.ect888.func210;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.ect888.bus.FunctionCommon;
import com.ect888.bus.impl.FunctionCommonImpl;
import com.ect888.config.Config;
import com.ect888.http.PoolClient;

/**
 * 2000210证通银行卡二要素认证服务
 * 示例代码
 * 
 * @author fanyj
 *
 */
public class Function2000210Test {
	
	static final String FUNC_NO="2000210";
	
	/**
	 * 银行卡号
	 * 会话密钥AES加密后参与签名
	 * post上述时 依次进行：a.会话秘钥AES加密；b.URLEncoder编码；c.Base64编码
	 * 
	 */
	String acctno="6228480771274173612";
	/**
	 * 证件号码 
	 * 会话密钥AES加密后参与签名
	 * post上述时 依次进行：a.会话秘钥AES加密；b.URLEncoder编码；c.Base64编码
	 * 
	 */
	String certseq="";
	
	/**
	 * 姓名
	 * 符合入参长度即可，不做技术限制
	 * 
	 *  参与签名
	 */
	String usernm="姓名";
	
	/**
	 * 来源渠道，填固定值“0”
	 * 
	 * 参与签名
	 */
	String sourcechnl="0";
	/**
	 * 业务发生地
	 * 符合入参长度即可，不做技术限制
	 * 
	 * 参与签名
	 */
	String placeid="00";
	/**
	 * 对照接口文档查看
	 * 符合入参长度即可，不做技术限制
	 * 
	 * 参与签名
	 */
	String biztyp="0541";
	/**
	 * 服务描述
	 * 符合入参长度即可，不做技术限制
	 * 
	 * 参与签名
	 */
	String biztypdesc="银行卡认证";
	/**
	 * 对公账户标识
	 * 0：对私   1：对公
	 * 缺省值为0，对公验证时必填
	 * 
	 * 参与签名
	 */
	String businessSign="0";
	/**
	 * 开户行代码
	 * 对公验证时必填
	 * 
	 * 参与签名
	 */
	String payeeOpenBankId="";
	/**
	 * 时间戳
	 * 
	 * 参与签名
	 */
	String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	
	/**
	 * 模拟调用
	 */
	public void doWork(){
			
		Map<String, String> params=buildParams();
		//加密加签,发起post请求，UrlEncodedFormEntity方式，选择相信服务端ssl证书，忽略证书认证
		String result = funcCommon.invoke(params);
			
		//解析返回数据并处理
		processResult(result);
	}
	
	/**
	 * 
	 * 将入参，按照http post上送和签名规则，放入map内
	 * 
	 * 
	 * （biztyp,biztypdesc,acctno,certseq,placeid,ptyacct,ptycd,sourcechnl,timestamp,key(会话密钥)）进行加签。
	 * 
	 * 其中，key前面的是按照字母排序的，key则是要最后附加上去。
	 * 
	 * 参与签名的银行卡号、证件号码为AES加密后的。生成的签名sign作为输入参数上送
	 * 
	 * 传上述参数时的银行卡号、证件号码要进行以下处理，步骤为：[a]用会话密钥加密(AES加密方法； [b]对a得到的结果进行URLEncoder.encode； [c]将b得到的结果转为base64字符串。 
	 * 
	 * @return 将入参，按照http post上送和签名规则，放入map内
	 */
	private Map<String, String> buildParams() {
		Map<String,String> params=new HashMap<String,String>();
		
		params.put(FunctionCommon.TO_AES_TO_URL_TO_BASE64_HEAD+"acctno", acctno);
		params.put(FunctionCommon.TO_AES_TO_URL_TO_BASE64_HEAD+"certseq", certseq);
		
		params.put(FunctionCommon.TO_SIGN_HEAD+"timestamp", timestamp);
		params.put(FunctionCommon.TO_SIGN_HEAD+"biztypdesc", biztypdesc);
		params.put(FunctionCommon.TO_SIGN_HEAD+"biztyp", biztyp);
		params.put(FunctionCommon.TO_SIGN_HEAD+"placeid", placeid);
		params.put(FunctionCommon.TO_SIGN_HEAD+"sourcechnl", sourcechnl);
		params.put(FunctionCommon.TO_SIGN_HEAD+"businessSign", businessSign);
		params.put(FunctionCommon.TO_SIGN_HEAD+"payeeOpenBankId", payeeOpenBankId);
		
		params.put(FunctionCommon.TO_SIGN_HEAD+"ptyacct",config.getPtyacct());
		params.put(FunctionCommon.TO_SIGN_HEAD+"ptycd",config.getPtycd());
		
		params.put("usernm", usernm);
		params.put("funcNo", FUNC_NO);
		
		return params;
	}

	/**
	 * json结果result的解析并处理
	 * 
	 * @param result
	 */
	private void processResult(String result) {
		 Json210 json=JSON.parseObject(result,Json210.class);
		 
		 if(null==json) {
			 log.error("返回报文解析为null,配置为"+JSON.toJSONString(config));
			 return;
		 }
		
		 if("0".equals(json.getError_no())) {//系统级调用成功
			 if(json.getResults().isEmpty()||null==json.getResults().get(0))//异常，系统级调用成功，却无结果，健壮性考虑，留此分支,联系服务端
				 throw new IllegalStateException("异常，系统级调用成功，却无结果，健壮性考虑，留此分支,联系服务端");
			 
			 Result210 re=json.getResults().get(0);
			 String status=re.getStatus();
			 if("00".equals(status)) {//订单成功结束,开始业务处理，此处示例打印主要业务应答结果
				 log.info("订单成功结束");
				 log.info("业务应答码respcd="+re.getRespcd());
				 log.info("业务应答信息respinfo="+re.getRespinfo());
				 log.info("发卡行信息phoneOperator="+re.getAccountname());
			 }else if("03".equals(status)) {//订单业务性失败结束,开始业务处理，此处示例打印主要业务应答结果
				 log.info("订单业务性失败结束");
				 log.info("业务应答码respcd="+re.getRespcd());
				 log.info("业务应答信息respinfo="+re.getRespinfo());
				 log.info("发卡行信息phoneOperator="+re.getAccountname());
			 }else if("01".equals(status)){//订单处理中，请稍后再轮询查询
				 log.info("订单处理中，请稍后再轮询查询");
			 }else {//异常，未知返回码，健壮性考虑，留此分支,联系服务端
				 throw new IllegalStateException("异常，未知返回码,联系服务端");
			 }
		 }else{//系统级调用失败，异常，查看入参或者联系服务端
			 throw new IllegalStateException("系统级调用失败，异常，查看入参或者联系服务端");
		 }
		
	}
	
	private Config config=Config.getInstance();
	
	private PoolClient client=PoolClient.getInstance();
	
	private FunctionCommonImpl funcCommon=FunctionCommonImpl.getInstance();
	
	private static Log log = LogFactory.getLog(Function2000210Test.class);
	
	@Test
	public void test() {
		try {
			doWork();
		}catch(RuntimeException e){
			log.error("运行时异常",e);
		}finally {
			//应用结束，关闭长连接及其连接池
			client.destroy();
			client.getConnManager().destroy();
		}
	}
	
	@Before
	public void before() {
		String log4jFileStr = "log4j.properties";
		PropertyConfigurator.configure(log4jFileStr);
	}
}
