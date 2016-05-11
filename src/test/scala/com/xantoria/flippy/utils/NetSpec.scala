package com.xantoria.flippy.utils

import java.net.InetAddress

import org.scalatest._

import com.xantoria.flippy.BaseSpec

class NetSpec extends BaseSpec {
  "The interpretRange util" should "work" in {
    val ip = "192.168.1.0"
    val prefix = 24

    val range = f"$ip%s/$prefix%d"
    val expected = (InetAddress.getByName(ip), prefix)
    val actual = Net.interpretRange(range)

    actual should be (expected)
  }

  "The addressInRange util" should "work" in {
    val addresses = List(
      "10.0.0.1",
      "10.100.0.1",
      "127.0.0.1",
      "192.168.1.1",
      "86.173.102.190",
      "86.173.102.199",
      "216.58.198.100"
    ) map { InetAddress.getByName(_) }

    val ranges = List(
      "192.168.1.0/24",
      "10.0.0.0/8",
      "10.0.0.0/16",
      "10.100.0.0/16",
      "86.173.102.0/24",
      "0.0.0.0/0"
    ) map { Net.interpretRange(_) }

    // Firstly, all addresses should be within their own /32 range
    addresses foreach {
      addr => withClue(s"addr in $addr/32?") {
        Net.addressInRange(addr, addr, 32) should be (true)
      }
    }

    // But not in each others'
    val (firstAddr, others) = (addresses.head, addresses.tail)
    others foreach {
      addr => withClue(s"$firstAddr not in $addr/32?") {
        Net.addressInRange(firstAddr, addr, 32) should be (false)
      }
    }

    // Here's the matrix of expected results for the range / IP combinations
    // Each row is an address and the expected results of checking their presence in each range
    val expected = List(
      List(false, true, true, false, false, true),   // 10.0.0.1
      List(false, true, false, true, false, true),   // 10.100.0.1
      List(false, false, false, false, false, true), // 127.0.0.1
      List(true, false, false, false, false, true),  // 192.168.1.1
      List(false, false, false, false, true, true),  // 86.173.102.190
      List(false, false, false, false, true, true),  // 86.173.102.199
      List(false, false, false, false, false, true)  // 216.58.198.100
    )

    addresses.zipWithIndex foreach {
      addrWithIndex => {
        val (addr, i) = addrWithIndex
        ranges.zipWithIndex foreach {
          rangeWithIndex => {
            val (range, j) = rangeWithIndex
            val (refAddr, prefix) = range

            withClue(s"$addr in $refAddr/$prefix?") {
              Net.addressInRange(addr, refAddr, prefix) should be (expected(i)(j))
            }
          }
        }
      }
    }
  }
}
