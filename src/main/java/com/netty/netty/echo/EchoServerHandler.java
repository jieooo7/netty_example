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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ctx.channel().writeAndFlush(Unpooled.buffer(EchoClient.SIZE).writeBytes("recived...\n\r".getBytes()));
		System.out.println(""+msg);
		
		ReferenceCountUtil.release(msg);//释放消息 ByteBuf in = (ByteBuf) msg; in.release();
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
		System.out.println("--------------------service完成");
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}


}

//
//Netty里四种主力的ByteBuf，
//
//		其中UnpooledHeapByteBuf 底下的byte[]能够依赖JVM GC自然回收；而UnpooledDirectByteBuf底下是DirectByteBuffer，如Java堆外内存扫盲贴所述，除了等JVM GC，最好也能主动进行回收；而PooledHeapByteBuf 和PooledDirectByteBuf，则必须要主动将用完的byte[]/ByteBuffer放回池里，否则内存就要爆掉。所以，Netty ByteBuf需要在JVM的GC机制之外，有自己的引用计数器和回收过程。
//
//
//
//		2. 引用计数器常识
//
//		计数器基于 AtomicIntegerFieldUpdater，为什么不直接用AtomicInteger？因为ByteBuf对象很多，如果都把int包一层AtomicInteger花销较大，而AtomicIntegerFieldUpdater只需要一个全局的静态变量。
//
//		所有ByteBuf的引用计数器初始值为1。
//
//		调用release()，将计数器减1，等于零时， deallocate()被调用，各种回收。
//
//		调用retain()，将计数器加1，即使ByteBuf在别的地方被人release()了，在本Class没喊cut之前，不要把它释放掉。
//
//		由duplicate(), slice()和order(ByteOrder)所创建的ByteBuf，与原对象共享底下的buffer，也共享引用计数器，所以它们经常需要调用retain()来显示自己的存在。
//
//		当引用计数器为0，底下的buffer已被回收，即使ByteBuf对象还在，对它的各种访问操作都会抛出异常。
//
//		3.谁来负责Release
//
//		在C时代，我们喜欢让malloc和free成对出现，而在Netty里，因为Handler链的存在，ByteBuf经常要传递到下一个Hanlder去而不复还，所以规则变成了谁是最后使用者，谁负责释放。
//
//		另外，更要注意的是各种异常情况，ByteBuf没有成功传递到下一个Hanlder，还在自己地界里的话，一定要进行释放。
//
//		3.1 InBound Message
//
//		在AbstractNioByteChannel.NioByteUnsafe.read() 处，配置好的ByteBufAllocator创建相应ByteBuf并调用 pipeline.fireChannelRead(byteBuf) 送入Handler链。
//
//		根据上面的谁最后谁负责原则，每一个Handler对消息可能有三种处理方式
//
//		对原消息不做处理，调用 ctx.fireChannelRead(msg)把原消息往下传，那不用做什么释放。
//
//		将原消息转化为新的消息并调用 ctx.fireChannelRead(newMsg)往下传，那必须把原消息release掉。
//
//		如果已经不再调用ctx.fireChannelRead(msg)传递任何消息，那更要把原消息release掉。
//
//		假设每一个Handler都把消息往下传，Handler并也不知道谁是启动Netty时所设定的Handler链的最后一员，所以Netty会在Handler链的最末补一个TailHandler，如果此时消息仍然是ReferenceCounted类型就会被release掉。
//
//		不过如果我们的业务Hanlder不再把消息往下传了，这个TailHandler就派不上用场。
//
//		3.2 OutBound Message
//
//		要发送的消息通常由应用所创建，并调用 ctx.writeAndFlush(msg) 进入Handler链。在每一个Handler中的处理类似InBound Message，最后消息会来到HeadHandler，再经过一轮复杂的调用，在flush完成后终将被release掉。
//
//		3.3 异常发生时的释放
//
//		多层的异常处理机制，有些异常处理的地方不一定准确知道ByteBuf之前释放了没有，可以在释放前加上引用计数大于0的判断避免异常；
//
//		有时候不清楚ByteBuf被引用了多少次，但又必须在此进行彻底的释放，可以循环调用reelase()直到返回true。
//
//
//
//		注意：调用writeAndFlush方法，会将ByteBuf的refCnt置为0，从而释放