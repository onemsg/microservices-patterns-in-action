package com.onemsg.vertxservice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class NBTimeServer {
    
    public static final int DEFAULT_PORT = 8900;

    private int port;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public NBTimeServer() {
        port = DEFAULT_PORT;
    }

    public NBTimeServer(int port) {
        this.port = port;
    }

    public void start() throws IOException{
        Selector selector = Selector.open();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        started.set(true);

        Thread.currentThread().setName("TimeServer-Thread");
        ThreadTest.printThreadStateAfter(Thread.currentThread(), 100);

        ssc.socket().bind(new InetSocketAddress(port));

        while (started.get()) {
            // 这里会打印当前线程状态
            selector.select(100);
            var keys = selector.selectedKeys().iterator();
            while(keys.hasNext()) {
                var key = keys.next();
                keys.remove();
                var nextReady = (ServerSocketChannel) key.channel();
                var out = nextReady.accept().socket().getOutputStream();

                // 休眠 200 毫秒
                ThreadTest.sleep(200);
                out.write(LocalDateTime.now().toString().getBytes());
                out.flush();
            }
        }
        selector.close();
        ssc.close();
    }

    public void stop() {
        started.set(false);
    }

    public static void main(String[] args) throws Exception{
        var timeServer = new NBTimeServer(6600);
        timeServer.start();
    }
}
