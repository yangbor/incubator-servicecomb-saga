package org.apache.servicecomb.saga.alpha.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class GrpcServerConfig {
  @Value("${alpha.server.host:0.0.0.0}")
  private String host;

  @Value("${alpha.server.port:8080}")
  private int port;

  @Value("${alpha.server.ssl.enable:false}")
  private boolean enable;

  @Value("${alpha.server.ssl.cert:server.cert}")
  private String cert;

  @Value("${alpha.server.ssl.key:server.key}")
  private String key;

  @Value("${alpha.server.ssl.enable:false}")
  private boolean mutalAuth;

  @Value("${alpha.server.ssl.clientCert:client.cert}")
  private String clientCert;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean isEnable() {
    return enable;
  }

  public String getCert() {
    return cert;
  }

  public String getKey() {
    return key;
  }

  public boolean isMutalAuth() {
    return mutalAuth;
  }

  public String getClientCert() {
    return clientCert;
  }
}


