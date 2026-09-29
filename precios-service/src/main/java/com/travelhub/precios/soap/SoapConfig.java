package com.travelhub.precios.soap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/** Publica el WSDL generado desde el XSD en /ws/cotizacion.wsdl (el bean se llama "cotizacion"). */
@Configuration
public class SoapConfig {

    @Bean
    public XsdSchema cotizacionSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/cotizacion.xsd"));
    }

    @Bean(name = "cotizacion")
    public DefaultWsdl11Definition cotizacion(XsdSchema cotizacionSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("CotizacionPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace(CotizacionEndpoint.NS);
        wsdl.setSchema(cotizacionSchema);
        return wsdl;
    }
}
