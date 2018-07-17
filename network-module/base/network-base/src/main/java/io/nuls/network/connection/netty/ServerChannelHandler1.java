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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.context.NulsContext;
import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.constant.NetworkParam;
import io.nuls.network.manager.BroadcastHandler;
import io.nuls.network.manager.ConnectionManager;
import io.nuls.network.manager.NodeManager;
import io.nuls.network.model.Node;
import io.nuls.network.protocol.message.HandshakeMessage;
import io.nuls.network.protocol.message.NetworkMessageBody;

import java.io.IOException;

/**
 * @author Vivi
 */

@ChannelHandler.Sharable
public class ServerChannelHandler1 extends ChannelInboundHandlerAdapter {

    private NodeManager nodeManager = NodeManager.getInstance();

    private NetworkParam networkParam = NetworkParam.getInstance();

    private BroadcastHandler broadcastHandler = BroadcastHandler.getInstance();

    private ConnectionManager connectionManager = ConnectionManager.getInstance();

    private ChannelHandlerContext ctx;

    private Channel channel;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("---------------------ServerChannelHandler1 channelRegistered  ");
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
        System.out.println("---------------------ServerChannelHandler1 channelActive  ");
        SocketChannel socketChannel = (SocketChannel) ctx.channel();
        NioChannelMap.add(ctx.channel().id().asLongText(), socketChannel);
        System.out.println(socketChannel.remoteAddress().getHostString());
        if (this.ctx != null) {

            System.out.println("this.ctx == ctx :" + (this.ctx == ctx));
        }
        if (this.channel != null) {
            System.out.println("this.channel == channel :" + (this.channel == ctx.channel()));
        }

        Node node = new Node(socketChannel.remoteAddress().getHostString(), socketChannel.remoteAddress().getPort(), Node.IN);
        node.setChannelId(channel.id().asLongText());
        NetworkMessageBody body = new NetworkMessageBody(NetworkConstant.HANDSHAKE_SEVER_TYPE, networkParam.getPort(),
                NulsContext.getInstance().getBestHeight(), NulsContext.getInstance().getBestBlock().getHeader().getHash(),
                socketChannel.remoteAddress().getHostString());
        HandshakeMessage handshakeMessage = new HandshakeMessage(body);
        broadcastHandler.broadcastToNode(handshakeMessage, node, false);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            Log.error(cause);
        }
        ctx.channel().close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("---------------------ServerChannelHandler1 channelRead  ");
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
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        System.out.println("---------------------ServerChannelHandler1 channelUnregistered  ");
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
