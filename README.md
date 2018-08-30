# FlowSeer

Implementation of FlowSeer in ONOS using Hoeffding Tree.

## Getting Started

These instructions will help you install and setup FlowSeer into your local machine for development and testing purposes.

### Prerequisites

You will need ONOS development version for it to work. Install ONOS according to this link [https://wiki.onosproject.org/display/ONOS/Development+Environment+Setup](https://wiki.onosproject.org/display/ONOS/Development+Environment+Setup). The following instructions will assume that ONOS is installed in the home directory `~/onos`. If ONOS is installed in some other directory then modify the path accordingly.

### Installing

FlowSeer is a modified version of the Reactive Forwarding App. You will need to replace that app in ONOS to install. Use the following steps to replace Reactive Forwarding App with FlowSeer.

Step 1: Delete the existing Reactive Forwarding App. You may keep a copy of it for backup purposes.

```
cd ~/onos/apps
rm fwd -d -r
```

Step 2: Download FlowSeer App from Github.

```
cd ~/onos/apps
git clone https://github.com/stainleebakhla/flowseer-fwd.git fwd
```

## Starting FlowSeer

Start ONOS using the following command

```
onos-buck run onos-local
```

For more options refer to this link [https://wiki.onosproject.org/display/ONOS/Development+Workflow+Options](https://wiki.onosproject.org/display/ONOS/Development+Workflow+Options).

Since FlowSeer is a part of the Reactive Forwarding App, which itself is a part of ONOS
controller, we first need to log into the ONOS CLI console. The following command does
the job for us

```
onos localhost
```

There are many commands available for use with FlowSeer. The following command lists
all the available commands present in FlowSeer along with a short description.

```
flowseer help
```

To start FlowSeer enter the following command

```
flowseer start
```

On issuing the above command, it takes us through an interview process of starting FlowSeer. The interview requires us to enter the following information:

1. _Packet Information:_ The number of data packets to capture for processing.
2. _Bandwidth:_ The bandwidth of the flow above which it is to be classified as an elephant flow.
3. _Time:_ The time duration above which a flow is to be classified as an elephant flow.
4. _Host Name / Address of Classifier:_ The network host name or the IP address of the computer which is running the classifier.
5. _Training Port:_ The port number where the training data is to be sent and its corresponding result is to be received.
6. _Testing Port:_ The port where testing data is to be sent and its corresponding result is to be received.

On entering the above information, FlowSeer will have started successfully. Any subsequent flows that will be captured by Reactive Forwarding App will be read by FlowSeer and will be sent for training or testing.
