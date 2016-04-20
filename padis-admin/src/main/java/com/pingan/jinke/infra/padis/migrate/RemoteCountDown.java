package com.pingan.jinke.infra.padis.migrate;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import com.google.common.collect.Sets;
import com.pingan.jinke.infra.padis.common.AbstractListenerManager;
import com.pingan.jinke.infra.padis.common.AbstractNodeListener;
import com.pingan.jinke.infra.padis.common.CoordinatorRegistryCenter;

public class RemoteCountDown extends AbstractListenerManager{
	
	private CountDownLatch countDown;
	
	private Set<String> nodeSet;

	public RemoteCountDown(String path,CoordinatorRegistryCenter coordinatorRegistryCenter) {
		super(path, coordinatorRegistryCenter,null);
		this.nodeSet = Sets.newHashSet();
	}

	@Override
	public void start() {		
		addDataListener(new CountDownListener() , instance);
	}
	
	public void fresh(){
		this.countDown = new CountDownLatch(1);
		nodeSet.clear();
		for(String node:this.nodeStorage.getNodePathChildrenKeys(instance)){
			nodeSet.add(instance+"/"+node);
		}
	}
	
	public void await(long timeout) throws InterruptedException{		
		this.countDown.await(timeout, TimeUnit.SECONDS);
	}
	
	class CountDownListener extends AbstractNodeListener{

		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if(Type.NODE_UPDATED == event.getType()){				
				nodeSet.remove(path);
				if(nodeSet.isEmpty()){
					countDown.countDown();
				}
			}
		}
	}
}