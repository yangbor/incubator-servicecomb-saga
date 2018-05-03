package org.apache.servicecomb.saga.omega.connector.grpc;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;
import org.junit.After;
import org.junit.AfterClass;

import io.grpc.Server;
import io.grpc.stub.StreamObserver;

public abstract class LoadBalancedClusterMessageSenderTestBase {
  protected static final int[] ports = {8080, 8090};

  protected static final Map<Integer, Server> servers = new HashMap<>();

  protected static final Map<Integer, Integer> delays = new HashMap<Integer, Integer>() {{
    put(8080, 0);
    put(8090, 800);
  }};

  protected static final Map<Integer, Queue<String>> connected = new HashMap<Integer, Queue<String>>() {{
    put(8080, new ConcurrentLinkedQueue<>());
    put(8090, new ConcurrentLinkedQueue<>());
  }};

  protected static final Map<Integer, Queue<TxEvent>> eventsMap = new HashMap<Integer, Queue<TxEvent>>() {{
    put(8080, new ConcurrentLinkedQueue<>());
    put(8090, new ConcurrentLinkedQueue<>());
  }};

  protected final List<String> compensated = new ArrayList<>();

  protected final String globalTxId = uniquify("globalTxId");

  protected final String localTxId = uniquify("localTxId");

  protected final String parentTxId = uniquify("parentTxId");

  protected final String compensationMethod = getClass().getCanonicalName();

  protected final TxEvent event = new TxEvent(EventType.TxStartedEvent, globalTxId, localTxId, parentTxId,
      compensationMethod, 0, "", 0, "blah");

  protected final String serviceName = uniquify("serviceName");

  protected final MessageSerializer serializer = objects -> objects[0].toString().getBytes();

  protected final MessageDeserializer deserializer = message -> new Object[] {new String(message)};

  protected final MessageHandler handler = (globalTxId, localTxId, parentTxId, compensationMethod,
      payloads) -> compensated.add(globalTxId);

  protected final String[] addresses = {"localhost:8080", "localhost:8090"};

  protected final MessageSender messageSender = newMessageSender(addresses);

  @AfterClass
  public static void tearDown() throws Exception {
    servers.values().forEach(Server::shutdown);
  }

  protected abstract MessageSender newMessageSender(String[] addresses);

  @After
  public void after() throws Exception {
    messageSender.onDisconnected();
    messageSender.close();
    eventsMap.values().forEach(Queue::clear);
    connected.values().forEach(Queue::clear);
  }

  protected static class MyTxEventService extends TxEventServiceImplBase {
    private final Queue<String> connected;
    private final Queue<TxEvent> events;
    private final int delay;

    private StreamObserver<GrpcCompensateCommand> responseObserver;

    protected MyTxEventService(Queue<String> connected, Queue<TxEvent> events, int delay) {
      this.connected = connected;
      this.events = events;
      this.delay = delay;
    }

    @Override
    public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcCompensateCommand> responseObserver) {
      this.responseObserver = responseObserver;
      connected.add("Connected " + request.getServiceName());
    }

    @Override
    public void onTxEvent(GrpcTxEvent request, StreamObserver<GrpcAck> responseObserver) {
      events.offer(new TxEvent(
          EventType.valueOf(request.getType()),
          request.getGlobalTxId(),
          request.getLocalTxId(),
          request.getParentTxId(),
          request.getCompensationMethod(),
          request.getTimeout(),
          request.getRetryMethod(),
          request.getRetries(),
          new String(request.getPayloads().toByteArray())));

      sleep();

      if (EventType.TxAbortedEvent.name().equals(request.getType())) {
        this.responseObserver.onNext(GrpcCompensateCommand
            .newBuilder()
            .setGlobalTxId(request.getGlobalTxId())
            .build());
      }

      if ("TxStartedEvent".equals(request.getType()) && request.getCompensationMethod().equals("reject")) {
        responseObserver.onNext(GrpcAck.newBuilder().setAborted(true).build());
      } else {
        responseObserver.onNext(GrpcAck.newBuilder().setAborted(false).build());
      }

      responseObserver.onCompleted();
    }

    private void sleep() {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        fail(e.getMessage());
      }
    }

    @Override
    public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
      connected.add("Disconnected " + request.getServiceName());
      responseObserver.onNext(GrpcAck.newBuilder().build());
      responseObserver.onCompleted();
    }
  }
}
