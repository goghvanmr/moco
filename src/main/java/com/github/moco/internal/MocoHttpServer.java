package com.github.moco.internal;

import com.github.moco.RequestMatcher;
import com.github.moco.Setting;
import com.github.moco.handler.ContentHandler;
import com.github.moco.setting.BaseSetting;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MocoHttpServer {
    private final int port;
    private ServerBootstrap bootstrap;
    private ChannelGroup allChannels;
    private final MocoHandler handler;

    public MocoHttpServer(int port) {
        this.port = port;
        this.handler = new MocoHandler();
    }

    public void start() {
        ChannelFactory factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("handler", handler);
                return pipeline;
            }
        });

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.bind(new InetSocketAddress(port));

        allChannels = new DefaultChannelGroup();
        allChannels.add(bootstrap.bind(new java.net.InetSocketAddress("localhost", port)));
    }

    public void stop() {
        allChannels.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }


    public Setting request(RequestMatcher matcher) {
        BaseSetting setting = new BaseSetting(matcher);
        this.handler.addSetting(setting);
        return setting;
    }

    public void response(String response) {
        this.handler.setAnyResponseHandler(new ContentHandler(response));
    }

}