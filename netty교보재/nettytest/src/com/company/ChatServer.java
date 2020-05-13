package com.company;

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
        this.port = port;  // 포트번호는 4484 지정해주자

    }


    public static void main(String[] args) {
	// write your code here
        try {
            new ChatServer(5555).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws Exception{

        /** 개인키 발급을 위한 코드 */
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .build();
        // SslContext를 사용하면
        EventLoopGroup bossGroup = new NioEventLoopGroup();     // 클라이언트의 연결을 수락하는 부모 스레드 그룹
        EventLoopGroup workerGroup = new NioEventLoopGroup();   // 연결된 클라이언트의 소켓으로부터 데이터 입출력 및 이벤트처리를 담당   NioEventLoopGroup 클래스 생성자의 인수로 사용된 숫자는 스레드 그룹내 생성할 최대 스레드 수
        /**
         NioEventLoopGroup 인자는 스레드 개수를 의미한다.
         */

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)          // 소켓 입출력 모드 설정 : NioServerSocketChannel 클래스 설정 => NIO 비동기식 방식으로 동작
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChatServerInitializer(sslCtx)); // 자식핸들러를 통해

            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 부트스트랩 설정 : 이벤트 루프, 채널의 전송모드, 채널 파이프라인으로 나뉜다.
    // epoll 은 입출력 다중화 기법 가장 빠름? 리눅스에서마 동작
    /**
     * 서버에서 수신한 데이터를 단일 스레드로 디비에 저장하는것
     * 소켓모드 NIO 사용 이런 소켓모드를 지원하는 소켓 채널, 데이터를 변환하여 db 저장하는 이벤트 핸들러, 포트 바인딩,
     */

}


