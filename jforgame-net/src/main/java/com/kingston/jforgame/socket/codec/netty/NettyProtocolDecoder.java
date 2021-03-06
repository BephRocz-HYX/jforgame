package com.kingston.jforgame.socket.codec.netty;

import java.util.List;

import com.kingston.jforgame.socket.codec.IMessageDecoder;
import com.kingston.jforgame.socket.codec.SerializerHelper;
import com.kingston.jforgame.socket.combine.CombineMessage;
import com.kingston.jforgame.socket.combine.Packet;
import com.kingston.jforgame.socket.message.Message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NettyProtocolDecoder extends ByteToMessageDecoder {

	private boolean remained = false;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (!remained) {
			if (in.readableBytes() < 4) {
				return;
			}
			remained = true;
		}
		IMessageDecoder msgDecoder = SerializerHelper.getInstance().getDecoder();
		// ----------------消息协议格式-------------------------
		// packetLength | moduleId | cmd | body
		// int short short byte[]
		int length = in.readInt();
		if (in.readableBytes() >= length) {
			// 消息元信息常量3表示消息body前面的两个字段，一个short表示module，一个byte表示cmd,
			final int metaSize = 3;
			short moduleId = in.readShort();
			byte cmd = in.readByte();
			byte[] body = new byte[length - metaSize];
			in.readBytes(body);
			Message msg = msgDecoder.readMessage(moduleId, cmd, body);

			if (moduleId > 0) {
				out.add(msg);
			} else { // 属于组合包
				CombineMessage combineMessage = (CombineMessage) msg;
				List<Packet> packets = combineMessage.getPackets();
				for (Packet packet : packets) {
					// 依次拆包反序列化为具体的Message
					out.add(Packet.asMessage(packet));
				}
			}
		}
	}

}
