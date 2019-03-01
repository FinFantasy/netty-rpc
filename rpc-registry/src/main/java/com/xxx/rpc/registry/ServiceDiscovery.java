package com.xxx.rpc.registry;

public interface ServiceDiscovery {

  /**
   * 根据服务名称查找服务
   */
  String discover(String serviceName);
}
