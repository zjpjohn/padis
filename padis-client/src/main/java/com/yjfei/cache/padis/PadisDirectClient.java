package com.yjfei.cache.padis;

import com.yjfei.cache.padis.IPadis;
import com.yjfei.cache.padis.PadisConfig;
import com.yjfei.cache.padis.common.CoordinatorRegistryCenter;
import com.yjfei.cache.padis.common.PoolConfig;
import com.yjfei.cache.padis.common.ZookeeperConfiguration;
import com.yjfei.cache.padis.core.Client;
import com.yjfei.cache.padis.core.ClusterInfoCacheManager;
import com.yjfei.cache.padis.core.PadisClientCommand;
import com.yjfei.cache.padis.core.PadisClientPoolManager;
import com.yjfei.cache.padis.storage.ZookeeperRegistryCenter;

public class PadisDirectClient implements IPadis{

	private PadisClientPoolManager poolManager;
	
	private ClusterInfoCacheManager clusterManager;
	
	private CoordinatorRegistryCenter regCenter;
	
	private PadisConfig config;
	
	public PadisDirectClient(PadisConfig config){
		this.config = new PadisConfig(config);	
		regCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration(this.config.getZkAddr(), "padis", 1000, 3000, 3));
		regCenter.init();		
		this.poolManager = new PadisClientPoolManager(this.config.getInstance(),regCenter,new PoolConfig(config));		
		this.clusterManager = new ClusterInfoCacheManager(this.config.getInstance(),regCenter);		
		init();
	}
	
	public PadisDirectClient(String zkAddr,String instance, String namespace){
		this(new PadisConfig(zkAddr, instance, namespace));
	}
	
	public void init(){
		//启动监听器,加载slot信息，注册custom
		this.clusterManager.init();
		//初始化连接池
		this.poolManager.init();
	}

	@Override
	public void setNameSpace(String nameSpace) {
		this.config.setNameSpace(nameSpace);
	}
	
	@Override
	public void close() {
		regCenter.close();
		poolManager.close();
	}

	private String makeKey(String key){
		return String.format("%s$%s$%s", config.getInstance(),config.getNameSpace(),key);
	}
	
	@Override
	public String set(final String key, final String value) throws Exception{
		final String targetKey = makeKey(key);
		return new PadisClientCommand<String>(clusterManager,poolManager,config.getMaxRedirections()){

			@Override
			public String execute(Client client) {
				return client.set(targetKey, value);
			}
			
		}.run(targetKey,true);
	}
	
	@Override	
	public String get(final  String key) throws Exception{
		final String targetKey = makeKey(key);
		return new PadisClientCommand<String>(clusterManager,poolManager,config.getMaxRedirections()){

			@Override
			public String execute(Client client) {
				return client.get(targetKey);
			}
			
		}.run(targetKey,false);
	}

	@Override
	public Long delete(String key) throws Exception {
		final String targetKey = makeKey(key);
		return new PadisClientCommand<Long>(clusterManager,poolManager,config.getMaxRedirections()){

			@Override
			public Long execute(Client client) {
				return client.delete(targetKey);
			}
			
		}.run(targetKey,false);
	}


	
}
