
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
 *         &lt;element name="FEParamGetActividadesResult" type="{http://ar.gov.afip.dif.FEV1/}FEActividadesResponse" minOccurs="0"/>
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
    "feParamGetActividadesResult"
})
@XmlRootElement(name = "FEParamGetActividadesResponse")
public class FEParamGetActividadesResponse {

    @XmlElement(name = "FEParamGetActividadesResult")
    protected FEActividadesResponse feParamGetActividadesResult;

    /**
     * Obtiene el valor de la propiedad feParamGetActividadesResult.
     * 
     * @return
     *     possible object is
     *     {@link FEActividadesResponse }
     *     
     */
    public FEActividadesResponse getFEParamGetActividadesResult() {
        return feParamGetActividadesResult;
    }

    /**
     * Define el valor de la propiedad feParamGetActividadesResult.
     * 
     * @param value
     *     allowed object is
     *     {@link FEActividadesResponse }
     *     
     */
    public void setFEParamGetActividadesResult(FEActividadesResponse value) {
        this.feParamGetActividadesResult = value;
    }

}
