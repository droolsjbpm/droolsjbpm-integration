package org.kie.services.client.serialization.jaxb.impl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;

import org.kie.api.command.Command;

@XmlRootElement(name="exception")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbExceptionResponse extends AbstractJaxbCommandResponse<Object> {

    @XmlElement
    @XmlSchemaType(name="string")
    private String message;
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String causeMessage;
    
    public JaxbExceptionResponse() {
    }
    
    public JaxbExceptionResponse(Exception e, Command<?> cmd) {
       super();
       this.commandName = cmd.getClass().getSimpleName();
       initializeExceptionInfo(e);
    }
    
    public JaxbExceptionResponse(Exception e, int i, Command<?> cmd) {
       super(i, cmd);
       initializeExceptionInfo(e);
    }
    
    private void initializeExceptionInfo(Exception e) { 
       this.message = e.getClass().getSimpleName() + " thrown with message '" + e.getMessage() + "'";
       if( e.getCause() != null ) { 
           Throwable t = e.getCause();
           this.causeMessage = t.getClass().getSimpleName() + " thrown with message '" + t.getMessage() + "'";
       }
    }
    
    public String getMessage() {
    	return message;
    }

    public String getCauseMessage() {
        return causeMessage;
    }

    @Override
    public Object getResult() {
        return message;
    }

}
