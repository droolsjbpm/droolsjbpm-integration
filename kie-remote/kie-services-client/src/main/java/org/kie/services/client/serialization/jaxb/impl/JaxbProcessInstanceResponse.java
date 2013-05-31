package org.kie.services.client.serialization.jaxb.impl;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.kie.api.command.Command;
import org.kie.api.definition.process.Process;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.services.client.serialization.jaxb.rest.JaxbRequestStatus;

@XmlRootElement(name="process-instance")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(value={JaxbProcess.class})
public class JaxbProcessInstanceResponse extends AbstractJaxbCommandResponse<ProcessInstance> implements ProcessInstance {

    @XmlElement(name="process-id")
    @XmlSchemaType(name="string")
    private String processId;

    @XmlElement
    @XmlSchemaType(name="long")
    private Long id;

    @XmlElement(name="process-name")
    @XmlSchemaType(name="string")
    private String processName;

    @XmlElement
    @XmlSchemaType(name="int")
    private Integer state; 

    @XmlElement
    private JaxbProcess process;
    
    @XmlElement(name="event-types")
    private List<String> eventTypes;
    
    @XmlElement
    private JaxbRequestStatus status;
    
    @XmlElement
    @XmlSchemaType(name="anyURI")
    private String url;
    
    public JaxbProcessInstanceResponse() { 
        // Default Constructor
    }

    public JaxbProcessInstanceResponse(ProcessInstance processInstance, int i, Command<?> cmd) { 
        super(i, cmd);
        initialize(processInstance);
    }

    public JaxbProcessInstanceResponse(ProcessInstance processInstance) { 
        initialize(processInstance);
    }

    public JaxbProcessInstanceResponse(ProcessInstance processInstance, HttpServletRequest request) { 
        initialize(processInstance);
        this.url = getUrl(request);
        this.status = JaxbRequestStatus.SUCCESS;
    }

    private void initialize(ProcessInstance processInstance) { 
        if( processInstance != null ) { 
            this.eventTypes = Arrays.asList(processInstance.getEventTypes());
            this.id = processInstance.getId();
            this.process = new JaxbProcess(processInstance.getProcess());
            this.processId = processInstance.getProcessId();
            this.processName = processInstance.getProcessName();
            this.state = processInstance.getState();
        }
    }

    private String getUrl(HttpServletRequest request) { 
        String url = request.getRequestURI();
        if( request.getQueryString() != null ) { 
            url += "?" + request.getQueryString();
        }
        return url;
    }
    
    @Override
    public String getProcessId() {
        return processId;
    }
    
    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public String getProcessName() {
        return processName;
    }
    
    @Override
    public int getState() {
        return state;
    }
    
    @Override
    public Process getProcess() {
        return process;
    }

    @Override
    public String[] getEventTypes() {
        return eventTypes.toArray(new String[eventTypes.size()]);
    }

    @Override
    public void signalEvent(String type, Object event) {
        String methodName = (new Throwable()).getStackTrace()[0].getMethodName();
        throw new UnsupportedOperationException( methodName + " is not supported on the JAXB " + ProcessInstance.class.getSimpleName() + " implementation.");
    }

    @Override
    public ProcessInstance getResult() {
        return this;
    }

    public JaxbRequestStatus getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public String toString() {
        final StringBuilder b = new StringBuilder( "ProcessInstance " );
        b.append( this.id );
        b.append( " [processId=" );
        b.append( this.processId );
        b.append( ",state=" );
        b.append( this.state );
        b.append( "]" );
        return b.toString();
    }
    
}
