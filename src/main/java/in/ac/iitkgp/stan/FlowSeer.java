/*
 * Copyright 2018-present Open Networking Laboratory
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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.fwd.ReactiveForwarding;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Skeletal ONOS application component.
 */
public class FlowSeer
{
    private CoreService coreService;
    private HostService hostService;
    private FlowRuleService flowRuleService;
    private Logger log;

    private FlowDataList flowDataList;
    private FlowStatisticsListener fsl;
    private ArffNetworkStreamGenerator trainingStream;
    private ArffNetworkStreamGenerator testingStream;
    private int k, bandwidth, time;
    private String host;
    private int trainingPort, testingPort;
    private boolean hasTrained;

    public FlowSeer(int k, int bandwidth, int time, String host, int trainingPort, int testingPort, CoreService coreService, HostService hostService, FlowRuleService flowRuleService, Logger log)
    {
        this.k = k;
        this.bandwidth = bandwidth;
        this.time = time;
        this.host = host;
        this.trainingPort = trainingPort;
        this.testingPort = testingPort;
        this.coreService = coreService;
        this.hostService = hostService;
        this.flowRuleService = flowRuleService;
        this.log = log;
        this.hasTrained = false;

        String attributes[][] = new String[3+k+k][2];

        attributes[0][0] = "src_port"; attributes[0][1] = "string";
        attributes[1][0] = "dst_port"; attributes[1][1] = "string";
        attributes[2][0] = "ip_protocol"; attributes[2][1] = "string";
        for (int i=3; i<k+3; i++)
        {
            attributes[i][0] = "packet_size_" + (i-2);
            attributes[i][1] = "numeric";
        }
        for (int i=k+3; i<2+k+k; i++)
        {
            attributes[i][0] = "inter_arrival_time_" + (i-k-2) + "_" + (i-k-1);
            attributes[i][1] = "numeric";
        }
        attributes[2+k+k][0] = "class";
        attributes[2+k+k][1] = "{X,E}";

        this.trainingStream = new ArffNetworkStreamGenerator(host, trainingPort, "flows", attributes);
        this.testingStream = new ArffNetworkStreamGenerator(host, testingPort, "flows", attributes);
    }

    public boolean start()
    {
        log.info("Starting FlowSeer...");

        flowDataList = new FlowDataList(this);
        fsl = new FlowStatisticsListener(flowDataList, coreService, log);
        flowRuleService.addListener(fsl);

        FlowData dummyFlowData = new FlowData(0, 0, 0, 0, k, 0, 0, null, null, 0, 0, 0, 0, null, null, 0, 0);
        String dummyData[] = convertFlowDataToString(dummyFlowData);

        if (this.trainingStream.connect(dummyData) && this.testingStream.connect(dummyData))
        {
            log.info("Started FlowSeer");
            return true;
        }
        else
        {
            log.info("Could not start FlowSeer.");
            stop();
            return false;
        }
    }

    public void stop()
    {
        log.info("Stopping FlowSeer...");

        if (fsl != null)
        {
            flowRuleService.removeListener(fsl);
            fsl = null;
            flowDataList = null;
            this.trainingStream.disconnect();
            this.testingStream.disconnect();
        }

        log.info("Stopped FlowSeer");
    }

    public boolean addPacketData(PacketContext context, TrafficSelector selector)
    {
        // NOTE: We will add the packet data to the flow data list only if it is received from the edge switch.
        // This is done to prevent the same packet being added to the list as the controller may receive
        // the same packet from multiple switches
        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();
        HostId id = HostId.hostId(ethPkt.getDestinationMAC());
        Host dst = hostService.getHost(id);

        if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId()))
        {
            if (!context.inPacket().receivedFrom().port().equals(dst.location().port()))
            {
                return flowDataList.add(context, selector, k);
            }
        }

        // Otherwise we will just install the rule just like normal reactive forwarding
        return true;
    }

    private String[] convertFlowDataToString(FlowData flowData)
    {
        String data[] = new String[3+k+k];
        data[0] = flowData.srcPort + "";
        data[1] = flowData.dstPort + "";
        data[2] = flowData.ipProtocol + "";
        for (int i=3; i<k+3; i++)
        {
            data[i] = flowData.packetSize[i-3] + "";
        }
        for (int i=k+3; i<2+k+k; i++)
        {
            data[i] = (flowData.packetTime[i-k-2] - flowData.packetTime[i-k-3]) + "";
        }

        data[2+k+k] = "X";

        return data;
    }

    public void train(FlowData flowData)
    {
        String data[] = convertFlowDataToString(flowData);

        // determining elephant flow or mice flow
        double duration = (double)(flowData.endtime - flowData.starttime) / (double)1000;
        duration = duration - ReactiveForwarding.DEFAULT_TIMEOUT;
        double bandwidth = (double) (flowData.bytes * 8) / (double) 1000000 / (double) duration;
        String result = "";
        if (bandwidth > this.bandwidth && duration > this.time)
        {
            result = "E";
        }
        else
        {
            result = "X";
        }

        data[2+k+k] = result;

        log.info("Sending data for training");
        log.info(Arrays.toString(data));
        trainingStream.sendData(data);
        hasTrained = true;
        /*
        boolean ans = trainingStream.receiveBoolean();
        if (ans)
        {
            log.info("Correct Answer");
        }
        else
        {
            log.info("Wrong Answer");
        }
        */
    }

    public void test(FlowData flowData)
    {
        if (hasTrained)
        {
            String data[] = convertFlowDataToString(flowData);

            log.info("Sending data for testing");
            log.info(Arrays.toString(data));
            testingStream.sendData(data);
        /*
        int ans = testingStream.receiveInt();
        char result = '-';
        if (ans == 0)
        {
            result = 'X';
            log.info(result + ": Mice flow.");
        }
        else if (ans == 1)
        {
            result = 'E';
            log.info(result + ": Elephant flow");
        }
        */
        }
    }

    public FlowDataList getFlowDataList()
    {
        return flowDataList;
    }
}
