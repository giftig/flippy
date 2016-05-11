package com.xantoria.flippy.utils

import java.net.InetAddress

object Net {
  /**
   * Interpret a string as an InetAddress range, converting it into an address and bit prefix len
   *
   * Interprets strings like 192.168.1.0/24 etc.
   */
  def interpretRange(s: String): (InetAddress, Int) = {
    val split = s.split("/")
    val addr = InetAddress.getByName(split(0))
    (addr, split(1).toInt)
  }

  /**
   * Check if the given address is in the range represented by the given reference and bit prefix
   */
  def addressInRange(addr: InetAddress, ref: InetAddress, prefix: Int): Boolean = {
    val mask = Array(
      0xff00 >>> math.min(prefix, 8),
      0xff00 >>> math.min(math.max(0, prefix - 8), 8),
      0xff00 >>> math.min(math.max(0, prefix - 16), 8),
      0xff00 >>> math.min(math.max(0, prefix - 24), 8)
    ) map { _ & 0xff }

    val maskedAddr = addr.getAddress.zip(mask) map { b => b._1 & b._2 }
    val maskedRef = ref.getAddress.zip(mask) map { b => b._1 & b._2 }

    !maskedAddr.zip(maskedRef).exists { b => b._1 != b._2 }
  }
}
