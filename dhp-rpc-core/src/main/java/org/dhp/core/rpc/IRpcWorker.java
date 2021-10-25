package org.dhp.core.rpc;

import org.dhp.common.rpc.Stream;

public interface IRpcWorker extends Runnable{
    boolean isOld(long time);
    int getSize();
    void execute(ServerCommand command, Stream stream, Message message, Session session);
    void dealTask(RpcExecutor task);
    void stop();
    void setAvg(String commandId, Double costAvg);
    double getAvg(String commandId);
    String getName();
    long getTotal();
}
