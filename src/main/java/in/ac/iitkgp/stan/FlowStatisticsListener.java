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

import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.slf4j.Logger;

import java.util.List;

public class FlowStatisticsListener implements FlowRuleListener
{
    private FlowDataList flowDataList;
    private CoreService coreService;
    private Logger log;

    FlowStatisticsListener(FlowDataList flowDataList, CoreService coreService, Logger log)
    {
        this.flowDataList = flowDataList;
        this.coreService = coreService;
        this.log = log;
    }

    @Override
    public void event(FlowRuleEvent event)
    {
        switch (event.type())
        {
            case RULE_REMOVED:
                FlowRule flowRule = event.subject();
                FlowEntry flowEntry = (FlowEntry) flowRule;
                if (flowEntry.appId() == coreService.getAppId("org.onosproject.fwd").id())
                {
                    getFlowStatistics(flowEntry);
                }
                break;

            default:
                break;
        }
    }

    private void getFlowStatistics(FlowEntry flowEntry)
    {
        // Timestamp of the flow

        long starttime = System.currentTimeMillis() - (1000 * flowEntry.life());
        long endtime = System.currentTimeMillis();
        
        // Bytes and packets of the flow
        long bytes = flowEntry.bytes();
        long packets = flowEntry.packets();

        // Switch input and output ports
        PortCriterion portCrit = (PortCriterion) flowEntry.selector().getCriterion(Criterion.Type.IN_PORT);
        int intfIn = (portCrit == null) ? 0 : (int) portCrit.port().toLong();
        List<Instruction> instructions = flowEntry.treatment().allInstructions();
        int intfOut = 0;
        for (Instruction instruction : instructions) {
            if (instruction.type() == Instruction.Type.OUTPUT) {
                Instructions.OutputInstruction outputInstruction = (Instructions.OutputInstruction) instruction;
                intfOut = (outputInstruction == null) ? 0 : (int) outputInstruction.port().toLong();
            }
        }

        // MAC addresses, Ethertype and VLAN
        EthCriterion ethCrit;
        ethCrit = (EthCriterion) flowEntry.selector().getCriterion(Criterion.Type.ETH_SRC);
        MacAddress srcMac = (ethCrit == null) ? MacAddress.valueOf("00:00:00:00:00:00") : ethCrit.mac();
        ethCrit = (EthCriterion) flowEntry.selector().getCriterion(Criterion.Type.ETH_DST);
        MacAddress dstMac = (ethCrit == null) ? MacAddress.valueOf("00:00:00:00:00:00") : ethCrit.mac();
        
        // Ethertype
        EthTypeCriterion ethTypeCrit = (EthTypeCriterion) flowEntry.selector().getCriterion(Criterion.Type.ETH_TYPE);
        int ethType = (ethTypeCrit == null) ? 0x0000 : ethTypeCrit.ethType().toShort();
        
        //VLAN
        VlanIdCriterion vlanCrit = (VlanIdCriterion) flowEntry.selector().getCriterion(Criterion.Type.VLAN_VID);
        int vlan = (vlanCrit == null) ? 0x0000 : vlanCrit.vlanId().toShort();

        // Getting IP Protocol
        IPProtocolCriterion protocolCrit = (IPProtocolCriterion) flowEntry.selector().getCriterion(Criterion.Type.IP_PROTO);
        int ipProtocol = (protocolCrit == null) ? (byte) 0xff : (byte) protocolCrit.protocol();

        IPDscpCriterion dscpCrit = (IPDscpCriterion) flowEntry.selector().getCriterion(Criterion.Type.IP_DSCP);
        int dscp = (dscpCrit == null) ? 0x00 : dscpCrit.ipDscp();
        IPEcnCriterion ecnCrit = (IPEcnCriterion) flowEntry.selector().getCriterion(Criterion.Type.IP_ECN);
        int ecn = (ecnCrit == null) ? 0x00 : ecnCrit.ipEcn();
        int tos = (byte) ((byte) (dscp << 2) | ecn);

        // Getting IP addresses
        IPCriterion srcIpCrit = (IPCriterion) flowEntry.selector().getCriterion(Criterion.Type.IPV4_SRC);
        IpAddress srcIp = (srcIpCrit == null) ? IpAddress.valueOf(0) : srcIpCrit.ip().address();
        IPCriterion dstIpCrit = (IPCriterion) flowEntry.selector().getCriterion(Criterion.Type.IPV4_DST);
        IpAddress dstIp = (dstIpCrit == null) ? IpAddress.valueOf(0) : dstIpCrit.ip().address();

        // Getting source port and destination port
        int srcPort = 0;
        int dstPort = 0;
        if (ipProtocol == IPv4.PROTOCOL_TCP)
        {
            TcpPortCriterion tcpCrit;
            tcpCrit = (TcpPortCriterion) flowEntry.selector().getCriterion(Criterion.Type.TCP_SRC);
            srcPort = (tcpCrit == null) ? 0 : tcpCrit.tcpPort().toInt();
            tcpCrit = (TcpPortCriterion) flowEntry.selector().getCriterion(Criterion.Type.TCP_DST);
            dstPort = (tcpCrit == null) ? 0 : tcpCrit.tcpPort().toInt();
        }
        else if (ipProtocol == IPv4.PROTOCOL_UDP)
        {
            UdpPortCriterion udpCrit;
            udpCrit = (UdpPortCriterion) flowEntry.selector().getCriterion(Criterion.Type.UDP_SRC);
            srcPort = (udpCrit == null) ? 0 : udpCrit.udpPort().toInt();
            udpCrit = (UdpPortCriterion) flowEntry.selector().getCriterion(Criterion.Type.UDP_DST);
            dstPort = (udpCrit == null) ? 0 : udpCrit.udpPort().toInt();
        }
        else if (ipProtocol == IPv4.PROTOCOL_ICMP)
        {
            IcmpTypeCriterion icmpTypeCrit = (IcmpTypeCriterion) flowEntry.selector().getCriterion(Criterion.Type.ICMPV4_TYPE);
            Short icmpType = (icmpTypeCrit == null) ? 0 : icmpTypeCrit.icmpType();
            IcmpCodeCriterion icmpCodeCrit = (IcmpCodeCriterion) flowEntry.selector().getCriterion(Criterion.Type.ICMPV4_CODE);
            Short icmpCode = (icmpCodeCrit == null) ? 0 : icmpCodeCrit.icmpCode();
            dstPort = 256 * icmpType + icmpCode;
        }

        FlowData data = new FlowData(
                starttime,
                endtime,
                bytes,
                packets,
                0,
                intfIn,
                intfOut,
                srcMac,
                dstMac,
                ethType,
                vlan,
                ipProtocol,
                tos,
                srcIp,
                dstIp,
                srcPort,
                dstPort
        );

        //data.print(log);
        //data.print(System.out);

        flowDataList.add(data, log);
    }
}
