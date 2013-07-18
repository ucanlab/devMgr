Device Mamanger (devMgr)
======
I. Background
Device manager (devMgr) is one of the sub-projects in community-based mobile cloud computing (CBMC) project.
CBMC project is funded by National Science Council (NSC) in Taiwan from 8/2012 to 7/2014.

II. Introduction
devMgr is designed to manage the devices in a cloudlet. 
devMgr needs to know that any device joins or leaves the cloudlet.
Moreover, devMgr needs to obtain the information of the devices in this cloudlet.
Thus, the node solicitation protocol (NSP) is designed for this purpose.

III. Node Solicitation Protocol
There are two roles in NSP: node (N) is the device. devMgr (M) is the device manager.
The message format of NSP is based on XML.

III.1. Message Type
When N want to join a cloudlet, N must send a join message to M in this cloudlet.
When N want to leave this cloudlet, N must send a leave message to M in this cloudlet.

III.2. Message Format
A. Join Message
<?xml version="1.0" encoding="UTF-8"?>
<status>
  <node action="join">
    <ip>1.2.3.4</ip>
    <port>1234</port>
    <cpu>1000</cpu>
    <mem>521</mem>
    <bat>100</bat>
  </node>
</status>

B. Leave Message
<?xml version="1.0" encoding="UTF-8"?>
<status>
  <node action="leave">
    <ip>1.2.3.4</ip>
    <port>1234</port>
  </node>
</status>

III.2 Message Handler
When M receives the join message of N, M must insert the information of N into the nodes list.
When M reveices the leave message of N, M must remove the information of N from the nodes list.
The format of nodes list is shown as follows.
<?xml version="1.0" encoding="UTF-8"?>
<nodelist>
  <node network="1.2.3.4:1234">
    <cpu>1000</cpu>
    <mem>512</mem>
    <bat>100</bat>
  </node>
  <node network="4.3.2.1:4321">
    <cpu>512</cpu>
    <mem>128</mem>
    <bat>80</bat>
  </node>
</nodelist>
