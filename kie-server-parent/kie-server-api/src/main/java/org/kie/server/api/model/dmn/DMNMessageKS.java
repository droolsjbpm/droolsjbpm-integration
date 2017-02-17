package org.kie.server.api.model.dmn;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.kie.dmn.core.api.DMNMessage;
import org.kie.dmn.feel.runtime.events.FEELEvent;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dmn-message")
//@XmlType(namespace="org.kie.server.api.model.dmn", name = "DMNMessageKS")
public class DMNMessageKS implements DMNMessage {
    
    public static enum DMNMessageSeverityKS {
        TRACE, INFO, WARN, ERROR;
        
        public static DMNMessageSeverityKS of(Severity value) {
            switch ( value ) {
                case ERROR:
                    return DMNMessageSeverityKS.ERROR;
                case INFO:
                    return DMNMessageSeverityKS.INFO;
                case TRACE:
                    return DMNMessageSeverityKS.TRACE;
                case WARN:
                    return DMNMessageSeverityKS.WARN;
                default:
                    return DMNMessageSeverityKS.ERROR;
            }
        }
        
        public Severity asSeverity() {
            switch ( this ) {
                case ERROR:
                    return Severity.ERROR;
                case INFO:
                    return Severity.INFO;
                case TRACE:
                    return Severity.TRACE;
                case WARN:
                    return Severity.WARN;
                default:
                    return Severity.ERROR;
            }
        }
    }

    @XmlElement(name="dmn-message-severity")
    private DMNMessageSeverityKS  severity;
    
    @XmlElement(name="message")
    private String    message;
    
    @XmlElement(name="source-id")
    private String    sourceId;

    public DMNMessageKS() {
        // no-arg constructor for marshalling
    }
    
    public static DMNMessageKS of(DMNMessage value) {
        DMNMessageKS res = new DMNMessageKS();
        res.severity = DMNMessageSeverityKS.of( value.getSeverity() );
        res.message = value.getMessage();
        res.sourceId = value.getSourceId();
        return res;
    }

    @Override
    public Severity getSeverity() {
        return severity.asSeverity();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public Throwable getException() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FEELEvent getFeelEvent() {
        throw new UnsupportedOperationException();
    }
}
