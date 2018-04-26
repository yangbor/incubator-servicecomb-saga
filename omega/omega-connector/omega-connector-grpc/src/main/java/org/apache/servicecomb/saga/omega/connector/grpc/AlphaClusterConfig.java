package org.apache.servicecomb.saga.omega.connector.grpc;

import java.util.List;

public class AlphaClusterConfig {
  private List<String> addresses;

  private boolean enableSSL;

  private boolean enableMutualAuth;

  private String cert;

  private String key;

  private String serverCert;

  public AlphaClusterConfig(List<String> addresses,
      boolean enableSSL,
      boolean enableMutualAuth,
      String cert,
      String key,
      String serverCert) {
    this.addresses = addresses;
    this.enableMutualAuth = enableMutualAuth;
    this.enableSSL = enableSSL;
    this.cert = cert;
    this.key = key;
    this.serverCert = serverCert;
  }

  public List<String> getAddresses() {
    return addresses;
  }

  public boolean isEnableSSL() {
    return enableSSL;
  }

  public boolean isEnableMutualAuth() {
    return enableMutualAuth;
  }

  public String getCert() {
    return cert;
  }

  public String getKey() {
    return key;
  }

  public String getServerCert() {
    return serverCert;
  }
}
