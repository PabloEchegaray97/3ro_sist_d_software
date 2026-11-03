
package ar.gov.afip.wsfe;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para anonymous complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="FEParamGetCondicionIvaReceptorResult" type="{http://ar.gov.afip.dif.FEV1/}CondicionIvaReceptorResponse" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "feParamGetCondicionIvaReceptorResult"
})
@XmlRootElement(name = "FEParamGetCondicionIvaReceptorResponse")
public class FEParamGetCondicionIvaReceptorResponse {

    @XmlElement(name = "FEParamGetCondicionIvaReceptorResult")
    protected CondicionIvaReceptorResponse feParamGetCondicionIvaReceptorResult;

    /**
     * Obtiene el valor de la propiedad feParamGetCondicionIvaReceptorResult.
     * 
     * @return
     *     possible object is
     *     {@link CondicionIvaReceptorResponse }
     *     
     */
    public CondicionIvaReceptorResponse getFEParamGetCondicionIvaReceptorResult() {
        return feParamGetCondicionIvaReceptorResult;
    }

    /**
     * Define el valor de la propiedad feParamGetCondicionIvaReceptorResult.
     * 
     * @param value
     *     allowed object is
     *     {@link CondicionIvaReceptorResponse }
     *     
     */
    public void setFEParamGetCondicionIvaReceptorResult(CondicionIvaReceptorResponse value) {
        this.feParamGetCondicionIvaReceptorResult = value;
    }

}
