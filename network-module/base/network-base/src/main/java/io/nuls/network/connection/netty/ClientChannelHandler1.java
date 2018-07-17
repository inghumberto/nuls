/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.network.connection.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.nuls.network.constant.NetworkParam;
import io.nuls.network.manager.ConnectionManager;
import io.nuls.network.manager.NodeManager;
import io.nuls.network.model.Node;

public class ClientChannelHandler1 extends ChannelInboundHandlerAdapter {

    private NodeManager nodeManager = NodeManager.getInstance();

    private ConnectionManager connectionManager = ConnectionManager.getInstance();

    private AttributeKey<Node> key = AttributeKey.valueOf("node");

    private NetworkParam networkParam = NetworkParam.getInstance();

    private ChannelHandlerContext ctx;

    private Channel channel;

    public ClientChannelHandler1() {

    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("---------------------ClientChannelHandler1 channelRegistered  ");
        SocketChannel socketChannel = (SocketChannel) ctx.channel();
        System.out.println(socketChannel.remoteAddress().getHostString());
        if (this.ctx == null) {
            this.ctx = ctx;
        }
        if (channel == null) {
            channel = ctx.channel();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("---------------------ClientChannelHandler1 channelActive  ");
        SocketChannel socketChannel = (SocketChannel) ctx.channel();
        System.out.println(socketChannel.remoteAddress().getHostString());
        if (this.ctx != null) {
            System.out.println("this.ctx == ctx :" + (this.ctx == ctx));
        }
        if (this.channel != null) {
            System.out.println("this.channel == channel :" + (this.channel == ctx.channel()));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        System.out.println("---------------------ClientChannelHandler1 channelInactive  ");
        SocketChannel socketChannel = (SocketChannel) ctx.channel();
        System.out.println(socketChannel.remoteAddress().getHostString());
        if (this.ctx != null) {
            System.out.println("this.ctx == ctx :" + (this.ctx == ctx));
        }
        if (this.channel != null) {
            System.out.println("this.channel == channel :" + (this.channel == ctx.channel()));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("---------------------ClientChannelHandler1 channelRead  ");
        SocketChannel socketChannel = (SocketChannel) ctx.channel();
        System.out.println(socketChannel.remoteAddress().getHostString());
        if (this.ctx != null) {
            System.out.println("this.ctx == ctx :" + (this.ctx == ctx));
        }
        if (this.channel != null) {
            System.out.println("this.channel == channel :" + (this.channel == ctx.channel()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("---------------------ClientChannelHandler1 channelUnregistered  ");
        SocketChannel socketChannel = (SocketChannel) ctx.channel();
        System.out.println(socketChannel.remoteAddress().getHostString());
        if (this.ctx != null) {
            System.out.println("this.ctx == ctx :" + (this.ctx == ctx));
        }
        if (this.channel != null) {
            System.out.println("this.channel == channel :" + (this.channel == ctx.channel()));
        }
    }
}
