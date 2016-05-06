package com.xantoria.flippy.utils

import java.net.InetAddress

object Net {
  def addressInRange(addr: InetAddress, ref: InetAddress, prefix: Int): Boolean = {
    val addrToLong: (InetAddress) => Long = {
      i: InetAddress => {
        val asBytes: Array[Byte] = i.getAddress
        asBytes(0) << 24 | asBytes(1) << 16 | asBytes(2) << 8 | asBytes(3)
      }
    }
    val addrVal = addrToLong(addr)
    val refVal = addrToLong(ref)
    val mask = (0xffffffff00000000L >> prefix) & 0xffffffff

    (addrVal & mask) == (refVal & mask)
  }
}
