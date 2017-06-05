/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.netty.netty.echo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClientHandler extends ChannelInboundHandlerAdapter {
	
	private final ByteBuf firstMessage;
	
	/**
	 * Creates a client-side handler.
	 */
	public EchoClientHandler() {
		firstMessage = Unpooled.buffer(EchoClient.SIZE);
//        for (int i = 0; i < firstMessage.capacity(); i ++) {
//            firstMessage.writeByte((byte) i);
//        }
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(firstMessage.writeBytes("start...\n".getBytes()));
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        ctx.write(msg);
		System.out.println("" + msg);
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
		System.out.println("----------------------client完成");
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
//ChannelInboundHandlerAdapter，看名字中的 IN，就是进入的意思，一般就是事件（event），比如当有数据到来时，channel被激活时或者不可用时，下面介绍几个最常用的。
//
//		channelActive
//		通道激活时触发，当客户端connect成功后，服务端就会接收到这个事件，从而可以把客户端的Channel记录下来，供后面复用
//		channelRead
//		这个必须用啊，当收到对方发来的数据后，就会触发，参数msg就是发来的信息，可以是基础类型，也可以是序列化的复杂对象。
//
//		channelReadComplete
//		channelRead执行后触发
//		exceptionCaught
//		出错是会触发，做一些错误处理
//
//
//		ChannelOutboundHandlerAdapter，看到了out，表示出去的动作，监听自己的IO操作，比如connect，bind等，在重写这个Adapter的方法时，记得执行super.xxxx，否则动作无法执行。
//
//		bind
//		服务端执行bind时，会进入到这里，我们可以在bind前及bind后做一些操作
//		connect
//		客户端执行connect连接服务端时进入

//	@Override
//	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
//		if (in.readableBytes() < 4) {
//			return;
//		}
//		从bytebuf转换成对象  MessageToMessageDecoder<ByteBuf>
//		out.add(new UnixTime(in.readUnsignedInt()));
//	}


//public class TimeEncoder extends MessageToByteEncoder<UnixTime> {
//	@Override
//	protected void encode(ChannelHandlerContext ctx, UnixTime msg, ByteBuf out) {
//		out.writeInt((int)msg.value());
//    从对象转换到bytebuf   MessageToByteEncoder<UnixTime>
//	}
//}