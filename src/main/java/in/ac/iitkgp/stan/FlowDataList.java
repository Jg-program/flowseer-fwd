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

import java.io.PrintStream;
import java.util.ArrayList;

import org.onlab.packet.IPacket;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.*;
import org.onosproject.net.packet.PacketContext;
import org.slf4j.Logger;

public class FlowDataList
{
    private FlowSeer flowSeer;
    private ArrayList<FlowData> arr;

    FlowDataList(FlowSeer flowSeer)
    {
        this.flowSeer = flowSeer;
        arr = new ArrayList<FlowData>();
    }

    public boolean add(PacketContext context, TrafficSelector selector, int k)
    {
        // MAC addresses, Ethertype and VLAN
        EthCriterion ethCrit;
        ethCrit = (EthCriterion) selector.getCriterion(Criterion.Type.ETH_SRC);
        MacAddress srcMac = (ethCrit == null) ? MacAddress.valueOf("00:00:00:00:00:00") : ethCrit.mac();
        ethCrit = (EthCriterion) selector.getCriterion(Criterion.Type.ETH_DST);
        MacAddress dstMac = (ethCrit == null) ? MacAddress.valueOf("00:00:00:00:00:00") : ethCrit.mac();

        // Getting IP Protocol
        IPProtocolCriterion protocolCrit = (IPProtocolCriterion) selector.getCriterion(Criterion.Type.IP_PROTO);
        int ipProtocol = (protocolCrit == null) ? (byte) 0xff : (byte) protocolCrit.protocol();

        // Getting IP addresses
        IPCriterion srcIpCrit = (IPCriterion) selector.getCriterion(Criterion.Type.IPV4_SRC);
        IpAddress srcIp = (srcIpCrit == null) ? IpAddress.valueOf(0) : srcIpCrit.ip().address();
        IPCriterion dstIpCrit = (IPCriterion) selector.getCriterion(Criterion.Type.IPV4_DST);
        IpAddress dstIp = (dstIpCrit == null) ? IpAddress.valueOf(0) : dstIpCrit.ip().address();

        // Getting source port and destination port
        int srcPort = 0;
        int dstPort = 0;
        if (ipProtocol == IPv4.PROTOCOL_TCP)
        {
            TcpPortCriterion tcpCrit;
            tcpCrit = (TcpPortCriterion) selector.getCriterion(Criterion.Type.TCP_SRC);
            srcPort = (tcpCrit == null) ? 0 : tcpCrit.tcpPort().toInt();
            tcpCrit = (TcpPortCriterion) selector.getCriterion(Criterion.Type.TCP_DST);
            dstPort = (tcpCrit == null) ? 0 : tcpCrit.tcpPort().toInt();
        }
        else if (ipProtocol == IPv4.PROTOCOL_UDP)
        {
            UdpPortCriterion udpCrit;
            udpCrit = (UdpPortCriterion) selector.getCriterion(Criterion.Type.UDP_SRC);
            srcPort = (udpCrit == null) ? 0 : udpCrit.udpPort().toInt();
            udpCrit = (UdpPortCriterion) selector.getCriterion(Criterion.Type.UDP_DST);
            dstPort = (udpCrit == null) ? 0 : udpCrit.udpPort().toInt();
        }
        else if (ipProtocol == IPv4.PROTOCOL_ICMP)
        {
            IcmpTypeCriterion icmpTypeCrit = (IcmpTypeCriterion) selector.getCriterion(Criterion.Type.ICMPV4_TYPE);
            Short icmpType = (icmpTypeCrit == null) ? 0 : icmpTypeCrit.icmpType();
            IcmpCodeCriterion icmpCodeCrit = (IcmpCodeCriterion) selector.getCriterion(Criterion.Type.ICMPV4_CODE);
            Short icmpCode = (icmpCodeCrit == null) ? 0 : icmpCodeCrit.icmpCode();
            dstPort = 256 * icmpType + icmpCode;
        }

        IPacket payload = context.inPacket().parsed().getPayload();
        int n = arr.size();

        // checking if the flow already exists in the list
        for (int i=0; i<n; i++)
        {
            FlowData fd = arr.get(i);

            if (
                    fd.srcIp.equals(srcIp)
                            &&
                    fd.srcPort == srcPort
                            &&
                    fd.dstIp.equals(dstIp)
                            &&
                    fd.dstPort == dstPort
                    )
            {
                // flow already exists
                // just update packet data
                for (int j=1; j<k; j++)
                {
                    if (fd.packetSize[j] < 0)
                    {
                        IPv4 p = (IPv4) payload;
                        fd.packetSize[j] = p.getTotalLength();
                        fd.packetTime[j] = System.nanoTime() / 1000;
                        if (j == k-1)
                        {
                            if (!fd.sentForTesting)
                            {
                                flowSeer.test(fd);
                                fd.sentForTesting = true;
                            }
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
                return true;
            }
        }

        // flow does not exist
        // add a new flow to the list
        FlowData flowData = new FlowData(
                0,
                0,
                0,
                0,
                k,
                0,
                0,
                srcMac,
                dstMac,
                0,
                0,
                0,
                0,
                srcIp,
                dstIp,
                srcPort,
                dstPort
        );

        if (payload instanceof IPv4)
        {
            IPv4 p = (IPv4) payload;
            flowData.packetSize[0] = p.getTotalLength();
            flowData.packetTime[0] = System.nanoTime() / 1000;
            arr.add(flowData);
            return false;
        }

        return true;
    }

    public void add(FlowData flowData, Logger log)
    {
        int n = arr.size();

        // checking if the flow already exists in the list
        for (int i=0; i<n; i++)
        {
            FlowData fd = arr.get(i);

            if (
                    fd.srcIp.equals(flowData.srcIp)
                            &&
                    fd.srcPort == flowData.srcPort
                            &&
                    fd.dstIp.equals(flowData.dstIp)
                            &&
                    fd.dstPort == flowData.dstPort
                    )
            {
                // flow already exists
                // just update the flow with higher byte count and packet count
                fd.bytes = Math.max(fd.bytes, flowData.bytes);
                fd.packets = Math.max(fd.packets, flowData.packets);
                fd.starttime = flowData.starttime;
                fd.endtime = flowData.endtime;
                fd.intfIn = flowData.intfIn;
                fd.intfOut = flowData.intfOut;
                fd.ethType = flowData.ethType;
                fd.vlan = flowData.vlan;
                fd.ipProtocol = flowData.ipProtocol;
                fd.tos = flowData.tos;
                fd.srcPort = flowData.srcPort;
                fd.dstPort = flowData.dstPort;

                // send this to the trainer
                if (!fd.sentForTraining)
                {
                    flowSeer.train(fd);
                    fd.sentForTraining = true;
                }
                return;
            }
        }

        // flow does not exist
        // add a new flow to the list
        log.info("WARNING-----THIS-LINE-SHOULD-NOT-HAVE-RUN-----WARNING");
        arr.add(flowData);
    }

    public void print(Logger log)
    {
        int n = arr.size();

        log.info("----------Flow Data List (Begin)----------");

        for (int i=0; i<n; i++)
        {
            arr.get(i).print(log);
        }

        log.info("----------Flow Data List (End)------------");
    }

    public void print(PrintStream out)
    {
        int n = arr.size();

        out.println("----------Flow Data List (Begin)----------");

        for (int i=0; i<n; i++)
        {
            arr.get(i).print(out);
        }

        out.println("----------Flow Data List (End)------------");
        out.println("Total flows: " + n);
    }
}
