package NT;

import java.net.InetAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class ChatServer {
    private final int port;
 
    public ChatServer(int port) {
        super();
        this.port = port;
    }
    
    public static void main(String[] args) throws Exception {
        //new ChatServer(5001).run();
    	
        try {
        	System.out.println("IP : " + InetAddress.getLocalHost());
            new ChatServer(8888).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void run() throws Exception {
        
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
            .build();
        // SslContext를 사용하면 
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChatServerInitializer(sslCtx));
            
            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}


//출처: https://altongmon.tistory.com/503?category=799997 [IOS를 Java]