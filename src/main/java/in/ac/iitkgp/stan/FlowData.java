/*
 * Copyright 2015 Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package in.ac.iitkgp.stan;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.io.PrintStream;
import java.util.Arrays;

import org.slf4j.Logger;

public class FlowData
{
    long starttime;
    long endtime;
    long bytes;
    long packets;
    int k;
    int packetSize[];
    long packetTime[];
    int intfIn;
    int intfOut;
    MacAddress srcMac;
    MacAddress dstMac;
    int ethType;
    int vlan;
    int ipProtocol;
    int tos;
    IpAddress srcIp;
    IpAddress dstIp;
    int srcPort;
    int dstPort;
    boolean sentForTraining;
    boolean sentForTesting;

    FlowData(
            long starttime,
            long endtime,
            long bytes,
            long packets,
            int k,
            int intfIn,
            int intfOut,
            MacAddress srcMac,
            MacAddress dstMac,
            int ethType,
            int vlan,
            int ipProtocol,
            int tos,
            IpAddress srcIp,
            IpAddress dstIp,
            int srcPort,
            int dstPort
    )
    {
        this.starttime = starttime;
        this.endtime = endtime;
        this.bytes = bytes;
        this.packets = packets;
        this.k = k;
        packetSize = new int[k];
        packetTime = new long[k];
        for (int i=0; i<k; i++)
        {
            packetSize[i] = -1;
            packetTime[i] = -1;
        }
        this.intfIn = intfIn;
        this.intfOut = intfOut;
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.ethType = ethType;
        this.vlan = vlan;
        this.ipProtocol = ipProtocol;
        this.tos = tos;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        sentForTraining = false;
        sentForTesting = false;
    }

    public void print(PrintStream out)
    {
        long KBytes = this.bytes / 1024;
        long MBytes = KBytes / 1024;
        long GBytes = MBytes / 1024;
        out.println("-----------------Flow Data (PrintStream)---");
        out.println("Start Time  : " + this.starttime);
        out.println("End Time    : " + this.endtime);
        out.println("Bytes       : " + this.bytes + " B\t" + KBytes + " KB\t" + MBytes + " MB\t" + GBytes + " GB");
        out.println("Packets     : " + this.packets);
        out.println("Port In     : " + this.intfIn);
        out.println("Port Out    : " + this.intfOut);
        out.println("Src Mac     : " + this.srcMac.toString());
        out.println("Dst Mac     : " + this.dstMac.toString());
        out.println("Eth Type    : " + this.ethType);
        out.println("VLAN        : " + this.vlan);
        out.println("IP Protocol : " + this.ipProtocol);
        out.println("TOS         : " + this.tos);
        out.println("Src IP      : " + this.srcIp.toString());
        out.println("Dst IP      : " + this.dstIp.toString());
        out.println("Src Port    : " + this.srcPort);
        out.println("Dst port    : " + this.dstPort);
        out.println("Packet Size : " + Arrays.toString(this.packetSize));
        out.println("Packet Time : " + Arrays.toString(this.packetTime));
    }
    
    public void print(Logger log)
    {
        long KBytes = this.bytes / 1024;
        long MBytes = KBytes / 1024;
        long GBytes = MBytes / 1024;
        log.info("-----------------Flow Data (Logger)--------");
        log.info("Start Time  : " + this.starttime);
        log.info("End Time    : " + this.endtime);
        log.info("Bytes       : " + this.bytes + " B\t" + KBytes + " KB\t" + MBytes + " MB\t" + GBytes + " GB");
        log.info("Packets     : " + this.packets);
        log.info("Port In     : " + this.intfIn);
        log.info("Port Out    : " + this.intfOut);
        log.info("Src Mac     : " + this.srcMac.toString());
        log.info("Dst Mac     : " + this.dstMac.toString());
        log.info("Eth Type    : " + this.ethType);
        log.info("VLAN        : " + this.vlan);
        log.info("IP Protocol : " + this.ipProtocol);
        log.info("TOS         : " + this.tos);
        log.info("Src IP      : " + this.srcIp.toString());
        log.info("Dst IP      : " + this.dstIp.toString());
        log.info("Src Port    : " + this.srcPort);
        log.info("Dst port    : " + this.dstPort);
        log.info("Packet Size : " + Arrays.toString(this.packetSize));
        log.info("Packet Time : " + Arrays.toString(this.packetTime));
    }
}
