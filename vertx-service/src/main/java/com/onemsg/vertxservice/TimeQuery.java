package com.onemsg.vertxservice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class TimeQuery {
    
    private static int DAYTIME_PORT = 13;

    private static Charset charset = StandardCharsets.US_ASCII;
    private static CharsetDecoder decoder = charset.newDecoder();

    private static ByteBuffer dbuf = ByteBuffer.allocateDirect(1024);

    private static void query(String host, int port) throws IOException {

        try (SocketChannel sc = SocketChannel.open()) {
            InetSocketAddress isa = new InetSocketAddress(
                InetAddress.getByName(host), port);

            sc.connect(isa);

            Thread.currentThread().setName("QueryClient-Thread");
            ThreadTest.printThreadStateAfter(Thread.currentThread(), 50);
            
            // 这里会打印当前线程状态，因为服务器会延迟200ms返回结果
            dbuf.clear();
            sc.read(dbuf);

            dbuf.flip();
            CharBuffer cb = decoder.decode(dbuf);
            System.out.print(isa + " : " + cb);
        }
    }

    public static void main(String[] args) throws Exception{
        query("localhost", 6600);
    }
}
