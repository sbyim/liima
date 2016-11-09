
package ch.mobi.xml.service.ch.mobi.maia.amw.maiaamwfederationservice.v1_0;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.8
 * Generated source version: 2.2
 * 
 */
@WebFault(name = "ValidationException", targetNamespace = "http://xml.mobi.ch/service/ch/mobi/maia/amw/v1_0/MaiaAmwFederationService/datatype")
public class ValidationException
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private ch.mobi.xml.datatype.common.commons.v3.ValidationException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public ValidationException(String message, ch.mobi.xml.datatype.common.commons.v3.ValidationException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public ValidationException(String message, ch.mobi.xml.datatype.common.commons.v3.ValidationException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: ch.mobi.xml.datatype.common.commons.v3.ValidationException
     */
    public ch.mobi.xml.datatype.common.commons.v3.ValidationException getFaultInfo() {
        return faultInfo;
    }

}
