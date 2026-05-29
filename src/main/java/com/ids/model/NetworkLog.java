package com.ids.model;
import java.sql.Timestamp;
public class NetworkLog {
    private String flowId;
    private String sourceIp;
    private String destinationIp;
    private int sourcePort;
    private int destinationPort;
    private String protocol;
    private String label;
    private Timestamp flowTimestamp;

    // The Superpower JSONB Column (Stores the other 68+ stats)
    private String flowStatisticsJson;

    // Constructor
    public NetworkLog(String flowId, String sourceIp, String destinationIp,
                      int sourcePort, int destinationPort, String protocol,
                      String label, Timestamp flowTimestamp, String flowStatisticsJson) {
        this.flowId = flowId;
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
        this.label = label;
        this.flowTimestamp = flowTimestamp;
        this.flowStatisticsJson = flowStatisticsJson;
    }

    // Getters
    public String getFlowId() { return flowId; }
    public String getSourceIp() { return sourceIp; }
    public String getDestinationIp() { return destinationIp; }
    public int getSourcePort() { return sourcePort; }
    public int getDestinationPort() { return destinationPort; }
    public String getProtocol() { return protocol; }
    public String getLabel() { return label; }
    public Timestamp getFlowTimestamp() { return flowTimestamp; }
    public String getFlowStatisticsJson() { return flowStatisticsJson; }
}
