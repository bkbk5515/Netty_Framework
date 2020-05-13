package com.company;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

import java.util.List;

public class ChatServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public ChatServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    /**
     채널 파이프라인 생성 == 소켓에서 바인드와 같이 소켓생성하는 부분이다.
     */
    @Override
    protected void initChannel(SocketChannel arg0) throws Exception {
        ChannelPipeline pipeline = arg0.pipeline();

        //pipeline.addLast(sslCtx.newHandler(arg0.alloc())); 보안을 강화.
        //pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(new ByteToMessageDecoder() {
            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                out.add(in.readBytes(in.readableBytes()));
            }
        });

        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());

        /**
         * 파이프라인에 chatserverhandler 붙혀서 ( 멀티탭같은 느낌 ) 핸들러가 이벤트 수신하고 처리해준다
         * */
        pipeline.addLast(new ChatServerHandler());


    }


}
